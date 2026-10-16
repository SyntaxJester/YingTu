package com.yingtu.plus.web;

import android.text.TextUtils;

import com.yingtu.plus.util.OkGo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 极简本地 HTTP 服务（仅绑定 127.0.0.1）。
 *
 * 为 WebHome 页面提供资源网关：{@code /webResource?url=<enc>&headers=<enc>&credentials=include}。
 * 作用是绕过浏览器 CORS、为图片/视频/字幕注入自定义请求头、透传 Range 分段。
 *
 * 设计目标是可编译、可运行、依赖最少（只用 ServerSocket + 现有 OkHttp 单例），
 * 不追求覆盖 NanoHTTPD 的全部能力。
 */
public class WebServer {

    private static volatile WebServer instance;

    private ServerSocket serverSocket;
    private int port = -1;
    private volatile boolean running;

    private WebServer() {}

    public static WebServer get() {
        if (instance == null) {
            synchronized (WebServer.class) {
                if (instance == null) instance = new WebServer();
            }
        }
        return instance;
    }

    /** 启动服务（幂等）。失败不抛出，只是 port 保持 -1。 */
    public synchronized void start() {
        if (running) return;
        try {
            serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            port = serverSocket.getLocalPort();
            running = true;
            // 把端口注册给 CatVod Proxy，spider 用它拼本地 /proxy 地址（图片代理 proxyImg / 网盘播放等依赖）
            try { com.github.catvod.Proxy.set(port); } catch (Throwable ignored) {}
            Thread accept = new Thread(this::loop, "yingtu-webserver");
            accept.setDaemon(true);
            accept.start();
        } catch (Throwable e) {
            running = false;
            port = -1;
        }
    }

    public int getPort() {
        return port;
    }

    /** 返回形如 {@code http://127.0.0.1:port} 的地址前缀，服务未启动时返回空串。 */
    public String getAddress(String path) {
        if (port <= 0) return "";
        String base = "http://127.0.0.1:" + port;
        if (TextUtils.isEmpty(path)) return base;
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    private void loop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                OkGo.io(() -> handle(socket));
            } catch (Throwable e) {
                if (!running) break;
            }
        }
    }

    private void handle(Socket socket) {
        try {
            socket.setSoTimeout(60000);
            PushbackInputStream in = new PushbackInputStream(socket.getInputStream(), 8192);
            OutputStream out = socket.getOutputStream();

            String requestLine = readLine(in);
            if (TextUtils.isEmpty(requestLine)) {
                socket.close();
                return;
            }
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "GET";
            String rawPath = parts.length > 1 ? parts[1] : "/";

            Map<String, String> reqHeaders = new LinkedHashMap<>();
            String headerLine;
            while (!TextUtils.isEmpty(headerLine = readLine(in))) {
                int idx = headerLine.indexOf(':');
                if (idx > 0) {
                    reqHeaders.put(headerLine.substring(0, idx).trim(),
                            headerLine.substring(idx + 1).trim());
                }
            }

            // 读取请求体（如果有 Content-Length）
            byte[] body = null;
            String cl = headerValue(reqHeaders, "Content-Length");
            if (!TextUtils.isEmpty(cl)) {
                try {
                    int len = Integer.parseInt(cl.trim());
                    body = readBytes(in, len);
                } catch (Throwable ignored) {
                }
            }

            String path = rawPath;
            String query = "";
            int q = rawPath.indexOf('?');
            if (q >= 0) {
                path = rawPath.substring(0, q);
                query = rawPath.substring(q + 1);
            }

            if ("OPTIONS".equalsIgnoreCase(method)) {
                writeStatus(out, 204, "No Content");
                writeCors(out);
                out.write("Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                socket.close();
                return;
            }

            if ("/webResource".equals(path)) {
                proxy(out, method, query, reqHeaders, body);
            } else if ("/proxy".equals(path)) {
                spiderProxy(out, query, reqHeaders);
            } else {
                writeStatus(out, 404, "Not Found");
                writeCors(out);
                out.write("Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            socket.close();
        } catch (Throwable e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** /webResource 代理：用 OkHttp 请求目标 URL，注入自定义 header，透传 Range，加 CORS。 */
    private void proxy(OutputStream out, String method, String query,
                       Map<String, String> reqHeaders, byte[] body) throws IOException {
        Map<String, String> params = parseQuery(query);
        String target = params.get("url");
        if (TextUtils.isEmpty(target)) {
            writeStatus(out, 400, "Bad Request");
            writeCors(out);
            out.write("Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            writeStatus(out, 400, "Bad Request");
            writeCors(out);
            out.write("Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        Request.Builder builder = new Request.Builder().url(target);

        // 注入 headers 参数里的自定义头
        Map<String, String> injected = new LinkedHashMap<>();
        String headersJson = params.get("headers");
        if (!TextUtils.isEmpty(headersJson)) {
            try {
                JsonObject obj = JsonParser.parseString(headersJson).getAsJsonObject();
                for (String k : obj.keySet()) {
                    if (!obj.get(k).isJsonNull()) injected.put(k, obj.get(k).getAsString());
                }
            } catch (Throwable ignored) {
            }
        }
        for (Map.Entry<String, String> e : injected.entrySet()) {
            builder.header(e.getKey(), e.getValue());
        }
        if (!hasKeyIgnoreCase(injected, "User-Agent")) {
            builder.header("User-Agent", DEFAULT_UA);
        }
        // 透传浏览器 Range（视频/音频分段）
        String range = headerValue(reqHeaders, "Range");
        if (!TextUtils.isEmpty(range)) builder.header("Range", range);

        okhttp3.RequestBody reqBody = null;
        String m = TextUtils.isEmpty(method) ? "GET" : method.toUpperCase();
        if (!"GET".equals(m) && !"HEAD".equals(m)) {
            String ct = injected.containsKey("Content-Type") ? injected.get("Content-Type") : "application/octet-stream";
            reqBody = okhttp3.RequestBody.create(body == null ? new byte[0] : body,
                    okhttp3.MediaType.parse(ct));
        }
        builder.method(m, reqBody);

        Response response = null;
        try {
            response = OkGo.client().newCall(builder.build()).execute();
            int code = response.code();
            writeStatus(out, code, response.message() == null ? "OK" : response.message());

            // 复制上游响应头（移除 hop-by-hop）
            Headers upstream = response.headers();
            for (String name : upstream.names()) {
                if (isHopByHop(name)) continue;
                for (String value : upstream.values(name)) {
                    out.write((name + ": " + value + "\r\n").getBytes(StandardCharsets.UTF_8));
                }
            }
            writeCors(out);
            out.write("Connection: close\r\n".getBytes(StandardCharsets.UTF_8));

            ResponseBody rb = response.body();
            if ("HEAD".equals(m) || rb == null) {
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                return;
            }
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            InputStream src = rb.byteStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = src.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Throwable e) {
            try {
                writeStatus(out, 502, "Bad Gateway");
                writeCors(out);
                out.write("Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Throwable ignored) {
            }
        } finally {
            if (response != null) response.close();
        }
    }

    /**
     * /proxy 端点：转发给 CatVod Spider 的 Proxy.proxy(params)。
     *
     * Spider（尤其图片代理 proxyImg、本地文件、网盘播放）会生成形如
     * http://127.0.0.1:port/proxy?... 的地址交给播放器/ImageView。
     * 返回约定 Object[]{ Integer code, String mime, InputStream body[, Map headers] }。
     */
    private void spiderProxy(OutputStream out, String query, Map<String, String> reqHeaders) throws IOException {
        Map<String, String> params = parseQuery(query);
        // 透传部分请求头（Range 等）给 spider
        String range = headerValue(reqHeaders, "Range");
        if (!TextUtils.isEmpty(range)) params.put("Range", range);
        Object[] rs = null;
        try {
            rs = com.yingtu.plus.api.SpiderManager.get().proxy(params);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (rs == null || rs.length < 3 || !(rs[0] instanceof Integer) || !(rs[2] instanceof InputStream)) {
            writeStatus(out, 500, "Proxy Error");
            writeCors(out);
            out.write("Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }
        int code = (Integer) rs[0];
        String mime = rs[1] == null ? "application/octet-stream" : rs[1].toString();
        InputStream body = (InputStream) rs[2];
        writeStatus(out, code, "OK");
        out.write(("Content-Type: " + mime + "\r\n").getBytes(StandardCharsets.UTF_8));
        // 可选的第 4 位 headers
        if (rs.length > 3 && rs[3] instanceof Map<?, ?> hs) {
            for (Map.Entry<?, ?> e : hs.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                String k = e.getKey().toString();
                if (isHopByHop(k)) continue;
                out.write((k + ": " + e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        writeCors(out);
        out.write("Connection: close\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        try {
            byte[] buf = new byte[16384];
            int n;
            while ((n = body.read(buf)) != -1) out.write(buf, 0, n);
            out.flush();
        } finally {
            try { body.close(); } catch (Throwable ignored) {}
        }
    }

    // ===== 工具 =====

    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0 Mobile Safari/537.36";

    private static boolean isHopByHop(String name) {
        String n = name.toLowerCase();
        return n.equals("connection") || n.equals("transfer-encoding")
                || n.equals("keep-alive") || n.equals("content-length")
                || n.equals("content-encoding") || n.equals("access-control-allow-origin");
    }

    private static void writeStatus(OutputStream out, int code, String msg) throws IOException {
        out.write(("HTTP/1.1 " + code + " " + msg + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void writeCors(OutputStream out) throws IOException {
        out.write("Access-Control-Allow-Origin: *\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("Access-Control-Allow-Credentials: true\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("Access-Control-Allow-Methods: GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("Access-Control-Allow-Headers: *\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("Access-Control-Expose-Headers: *\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String headerValue(Map<String, String> headers, String key) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    private static boolean hasKeyIgnoreCase(Map<String, String> map, String key) {
        for (String k : map.keySet()) if (k.equalsIgnoreCase(key)) return true;
        return false;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
        if (TextUtils.isEmpty(query)) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String k = pair.substring(0, idx);
            String v = pair.substring(idx + 1);
            try {
                map.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
            } catch (Throwable ignored) {
            }
        }
        return map;
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c == '\r') continue;
            buf.write(c);
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(InputStream in, int len) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[Math.min(len, 16384)];
        int remaining = len;
        while (remaining > 0) {
            int n = in.read(tmp, 0, Math.min(remaining, tmp.length));
            if (n == -1) break;
            buf.write(tmp, 0, n);
            remaining -= n;
        }
        return buf.toByteArray();
    }

    /** 便捷：拿到所有本地地址（调试用）。 */
    public List<String> addresses() {
        List<String> list = new ArrayList<>();
        if (port > 0) list.add("http://127.0.0.1:" + port);
        return list;
    }
}

package com.yingtu.plus.web;

import android.text.TextUtils;

import com.yingtu.plus.util.OkGo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * WebHome 的 Native 网络请求实现（对应 fm.req / fongmi.net.request）。
 *
 * 用 OkHttp 直接发起请求，绕过浏览器 CORS，支持自定义 header、
 * 自动 gzip/deflate 解码、responseType=text/json/base64。
 */
public class WebCall {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .followRedirects(true).followSslRedirects(true).build();

    public static String request(JsonObject payload) {
        Response response = null;
        String url = safe(payload, "url");
        String method = getMethod(safe(payload, "method"));
        try {
            String body = safe(payload, "body");
            String responseType = safe(payload, "responseType");
            int timeout = getTimeout(payload);

            Request.Builder builder = new Request.Builder().url(url);
            boolean hasEncoding = false;
            if (payload.has("headers") && payload.get("headers").isJsonObject()) {
                JsonObject headers = payload.getAsJsonObject("headers");
                for (String k : headers.keySet()) {
                    if (headers.get(k).isJsonNull()) continue;
                    builder.header(k, headers.get(k).getAsString());
                    if ("Accept-Encoding".equalsIgnoreCase(k)) hasEncoding = true;
                }
            }
            if (!hasEncoding) builder.header("Accept-Encoding", "gzip");

            builder.method(method, getBody(method, body, builder.build().headers()));
            Request request = builder.build();
            response = client(timeout).newCall(request).execute();
            return toJson(response, responseType);
        } catch (Throwable e) {
            return error(e);
        } finally {
            if (response != null) response.close();
        }
    }

    private static int getTimeout(JsonObject payload) {
        try {
            return Math.max(payload.get("timeout").getAsInt(), 1);
        } catch (Exception e) {
            return 30;
        }
    }

    private static OkHttpClient client(int timeout) {
        long millis = TimeUnit.SECONDS.toMillis(timeout);
        return CLIENT.newBuilder()
                .connectTimeout(millis, TimeUnit.MILLISECONDS)
                .readTimeout(millis, TimeUnit.MILLISECONDS)
                .writeTimeout(millis, TimeUnit.MILLISECONDS).build();
    }

    private static String getMethod(String method) {
        return TextUtils.isEmpty(method) ? "GET" : method.toUpperCase();
    }

    private static RequestBody getBody(String method, String body, Headers headers) {
        if ("GET".equals(method) || "HEAD".equals(method)) return null;
        String type = headers.get("Content-Type");
        MediaType mediaType = MediaType.parse(TextUtils.isEmpty(type) ? "text/plain; charset=utf-8" : type);
        return RequestBody.create(body == null ? "" : body, mediaType);
    }

    private static String toJson(Response response, String responseType) throws Exception {
        byte[] raw = response.body() == null ? new byte[0] : readAll(response.body().byteStream());
        byte[] bytes = decode(response.header("Content-Encoding"), raw);
        JsonObject object = new JsonObject();
        object.addProperty("ok", response.isSuccessful());
        object.addProperty("status", response.code());
        object.addProperty("url", response.request().url().toString());
        object.add("headers", headers(response.headers()));
        object.add("cookies", gsonArray(response.headers().values("Set-Cookie")));
        String body = new String(bytes, StandardCharsets.UTF_8);
        if ("base64".equals(responseType)) object.addProperty("body", base64(bytes));
        else if ("json".equals(responseType)) object.add("body", parse(body));
        else object.addProperty("body", body);
        return object.toString();
    }

    private static com.google.gson.JsonArray gsonArray(java.util.List<String> values) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (String v : values) arr.add(v);
        return arr;
    }

    private static JsonElement parse(String body) {
        try {
            return JsonParser.parseString(body);
        } catch (Throwable e) {
            return new JsonObject();
        }
    }

    private static String base64(byte[] bytes) {
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
    }

    private static byte[] decode(String encoding, byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) return bytes;
        if (hasEncoding(encoding, "br")) throw new IOException("Unsupported Content-Encoding: br");
        if (hasEncoding(encoding, "gzip") || isGzip(bytes)) return gunzip(bytes);
        if (hasEncoding(encoding, "deflate")) return inflate(bytes);
        return bytes;
    }

    private static boolean hasEncoding(String encoding, String value) {
        if (TextUtils.isEmpty(encoding)) return false;
        for (String item : encoding.split(",")) if (value.equalsIgnoreCase(item.trim())) return true;
        return false;
    }

    private static boolean isGzip(byte[] bytes) {
        return bytes.length > 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
    }

    private static byte[] gunzip(byte[] bytes) throws Exception {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return readAll(gzip);
        }
    }

    private static byte[] inflate(byte[] bytes) throws Exception {
        try {
            return inflate(bytes, true);
        } catch (Throwable e) {
            return inflate(bytes, false);
        }
    }

    private static byte[] inflate(byte[] bytes, boolean nowrap) throws Exception {
        try (InflaterInputStream input = new InflaterInputStream(
                new ByteArrayInputStream(bytes), new Inflater(nowrap))) {
            return readAll(input);
        }
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream stream = input) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static JsonObject headers(Headers headers) {
        JsonObject object = new JsonObject();
        for (String name : headers.names()) {
            if (headers.values(name).size() == 1) object.addProperty(name, headers.get(name));
            else object.add(name, gsonArray(headers.values(name)));
        }
        return object;
    }

    private static String error(Throwable e) {
        JsonObject object = new JsonObject();
        object.addProperty("ok", false);
        object.addProperty("status", 500);
        object.addProperty("body", "");
        object.addProperty("error", e.getMessage());
        object.add("headers", new JsonObject());
        return object.toString();
    }

    private static String safe(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }

    public static JsonObject object(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Throwable e) {
            return new JsonObject();
        }
    }
}

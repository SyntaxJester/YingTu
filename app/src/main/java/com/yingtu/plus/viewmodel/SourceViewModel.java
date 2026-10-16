package com.yingtu.plus.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.yingtu.plus.api.ApiConfig;
import com.yingtu.plus.api.JsonParserHelper;
import com.yingtu.plus.api.VodSpider;
import com.yingtu.plus.bean.Movie;
import com.yingtu.plus.bean.MovieSort;
import com.yingtu.plus.bean.SourceBean;
import com.yingtu.plus.bean.VodInfo;
import com.yingtu.plus.util.OkGo;
import com.github.catvod.crawler.Spider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 承载首页、分类、详情、搜索的数据加载。
 *
 * 关键修复：区分站点类型。
 *  - type==3 且 api 以 csp_ 开头：CSP 站点，调用 CatVod Spider（JAR/JS）；
 *    homeContent / categoryContent / detailContent / searchContent 都返回标准 TVBox JSON，
 *    复用 {@link JsonParserHelper} 解析。
 *  - 其余（type 0/1 的 HTTP JSON 源）：按原有 ?ac=class / ?ac=detail / ?wd= 走 OkHttp。
 *
 * 之前首页空白、切换站源无效，就是因为只实现了 HTTP 分支，CSP 站点（本例配置几乎全是 csp_）
 * 直接请求 api 字符串（"csp_XxxGuard"）当 URL，必然失败。
 */
public class SourceViewModel extends ViewModel {

    public final MutableLiveData<List<MovieSort>> sortResult = new MutableLiveData<>();
    public final MutableLiveData<List<Movie>> listResult = new MutableLiveData<>();
    public final MutableLiveData<List<Movie>> searchResult = new MutableLiveData<>();
    public final MutableLiveData<VodInfo> detailResult = new MutableLiveData<>();

    /** 加载首页分类（CSP: homeContent(true)；HTTP: ?ac=class） */
    public void getSort() {
        SourceBean source = ApiConfig.get().getHomeSource();
        if (source == null) { sortResult.postValue(null); return; }
        if (VodSpider.isCsp(source)) {
            VodSpider.executor().execute(() -> {
                try {
                    Spider spider = VodSpider.get(source);
                    String home = spider.homeContent(true);
                    List<MovieSort> sorts = JsonParserHelper.parseSortList(home);
                    sortResult.postValue(sorts);
                    // 首页推荐影片：优先 homeContent 内嵌 list，否则 homeVideoContent
                    List<Movie> recommend = JsonParserHelper.parseMovieList(home, source.getKey());
                    if (recommend.isEmpty()) {
                        try {
                            String hv = spider.homeVideoContent();
                            recommend = JsonParserHelper.parseMovieList(hv, source.getKey());
                        } catch (Throwable ignored) {}
                    }
                    listResult.postValue(recommend);
                } catch (Throwable e) {
                    e.printStackTrace();
                    sortResult.postValue(null);
                }
            });
            return;
        }
        get(source.getApi() + "?ac=class", body ->
                sortResult.postValue(JsonParserHelper.parseSortList(body)));
    }

    /** 加载某个分类的影片列表（可带筛选 extend） */
    public void getList(String typeId, int page, HashMap<String, String> extend) {
        SourceBean source = ApiConfig.get().getHomeSource();
        if (source == null) { listResult.postValue(null); return; }
        final HashMap<String, String> ext = extend == null ? new HashMap<>() : extend;
        if (VodSpider.isCsp(source)) {
            VodSpider.executor().execute(() -> {
                try {
                    Spider spider = VodSpider.get(source);
                    String json = spider.categoryContent(typeId, String.valueOf(page), true, ext);
                    listResult.postValue(JsonParserHelper.parseMovieList(json, source.getKey()));
                } catch (Throwable e) {
                    e.printStackTrace();
                    listResult.postValue(null);
                }
            });
            return;
        }
        StringBuilder url = new StringBuilder(source.getApi()).append("?ac=detail&t=").append(typeId).append("&pg=").append(page);
        for (java.util.Map.Entry<String, String> en : ext.entrySet()) {
            if (en.getValue() != null && !en.getValue().isEmpty()) {
                url.append('&').append(en.getKey()).append('=').append(en.getValue());
            }
        }
        get(url.toString(), body ->
                listResult.postValue(JsonParserHelper.parseMovieList(body, source.getKey())));
    }

    /** 兼容旧调用 */
    public void getList(String typeId, int page) {
        getList(typeId, page, null);
    }

    /** 加载影片详情 */
    public void getDetail(String sourceKey, String vodId) {
        SourceBean source = ApiConfig.get().getSource(sourceKey);
        if (source == null) { detailResult.postValue(null); return; }
        if (VodSpider.isCsp(source)) {
            VodSpider.executor().execute(() -> {
                try {
                    Spider spider = VodSpider.get(source);
                    java.util.List<String> ids = new java.util.ArrayList<>();
                    ids.add(vodId);
                    String json = spider.detailContent(ids);
                    detailResult.postValue(JsonParserHelper.parseVodInfo(json, sourceKey));
                } catch (Throwable e) {
                    e.printStackTrace();
                    detailResult.postValue(null);
                }
            });
            return;
        }
        String url = source.getApi() + "?ac=detail&ids=" + vodId;
        get(url, body ->
                detailResult.postValue(JsonParserHelper.parseVodInfo(body, sourceKey)));
    }

    /** 搜索 */
    public void search(String keyword) {
        SourceBean source = ApiConfig.get().getHomeSource();
        if (source == null) { searchResult.postValue(null); return; }
        if (VodSpider.isCsp(source)) {
            VodSpider.executor().execute(() -> {
                try {
                    Spider spider = VodSpider.get(source);
                    String json = spider.searchContent(keyword, false);
                    searchResult.postValue(JsonParserHelper.parseMovieList(json, source.getKey()));
                } catch (Throwable e) {
                    e.printStackTrace();
                    searchResult.postValue(null);
                }
            });
            return;
        }
        String url = source.getApi() + "?wd=" + keyword;
        get(url, body ->
                searchResult.postValue(JsonParserHelper.parseMovieList(body, source.getKey())));
    }

    /** 跨源搜索（在所有可搜索站点上并发搜索，结果边到边追加）。 */
    public void searchAll(String keyword) {
        List<SourceBean> sources = ApiConfig.get().getSourceBeanList();
        for (SourceBean source : sources) {
            if (!source.isSearchable()) continue;
            VodSpider.executor().execute(() -> {
                try {
                    List<Movie> movies;
                    if (VodSpider.isCsp(source)) {
                        Spider spider = VodSpider.get(source);
                        String json = spider.searchContent(keyword, false);
                        movies = JsonParserHelper.parseMovieList(json, source.getKey());
                    } else {
                        Request req = new Request.Builder().url(source.getApi() + "?wd=" + keyword).build();
                        Response resp = OkGo.client().newCall(req).execute();
                        String body = resp.body() != null ? resp.body().string() : "";
                        movies = JsonParserHelper.parseMovieList(body, source.getKey());
                    }
                    if (!movies.isEmpty()) appendSearch(movies);
                } catch (Throwable ignored) {}
            });
        }
    }

    private synchronized void appendSearch(List<Movie> add) {
        List<Movie> cur = searchResult.getValue();
        List<Movie> merged = cur == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(cur);
        merged.addAll(add);
        searchResult.postValue(merged);
    }

    private interface OnBody { void onBody(String body); }

    private void get(String url, OnBody cb) {
        Request request = new Request.Builder().url(url).build();
        OkGo.client().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                OkGo.main(() -> cb.onBody(""));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                OkGo.main(() -> cb.onBody(body));
            }
        });
    }
}

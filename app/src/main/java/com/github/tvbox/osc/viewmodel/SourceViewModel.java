package com.github.tvbox.osc.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.api.JsonParserHelper;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.util.OkGo;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 承载首页、分类、详情、搜索的数据加载逻辑。
 *
 * 结果通过 LiveData 回传给 UI，网络在 OkHttp 异步线程执行，
 * 回调里再切回主线程更新 LiveData。
 */
public class SourceViewModel extends ViewModel {

    public final MutableLiveData<List<MovieSort>> sortResult = new MutableLiveData<>();
    public final MutableLiveData<List<Movie>> listResult = new MutableLiveData<>();
    public final MutableLiveData<List<Movie>> searchResult = new MutableLiveData<>();
    public final MutableLiveData<VodInfo> detailResult = new MutableLiveData<>();

    /** 加载首页分类 */
    public void getSort() {
        SourceBean source = ApiConfig.get().getHomeSource();
        if (source == null) {
            sortResult.postValue(null);
            return;
        }
        get(source.getApi() + "?ac=class", body ->
                sortResult.postValue(JsonParserHelper.parseSortList(body)));
    }

    /** 加载某个分类的影片列表 */
    public void getList(String typeId, int page) {
        SourceBean source = ApiConfig.get().getHomeSource();
        if (source == null) {
            listResult.postValue(null);
            return;
        }
        String url = source.getApi() + "?ac=detail&t=" + typeId + "&pg=" + page;
        get(url, body ->
                listResult.postValue(JsonParserHelper.parseMovieList(body, source.getKey())));
    }

    /** 加载影片详情 */
    public void getDetail(String sourceKey, String vodId) {
        SourceBean source = ApiConfig.get().getSource(sourceKey);
        if (source == null) {
            detailResult.postValue(null);
            return;
        }
        String url = source.getApi() + "?ac=detail&ids=" + vodId;
        get(url, body ->
                detailResult.postValue(JsonParserHelper.parseVodInfo(body, sourceKey)));
    }

    /** 搜索 */
    public void search(String keyword) {
        SourceBean source = ApiConfig.get().getHomeSource();
        if (source == null) {
            searchResult.postValue(null);
            return;
        }
        String url = source.getApi() + "?wd=" + keyword;
        get(url, body ->
                searchResult.postValue(JsonParserHelper.parseMovieList(body, source.getKey())));
    }

    private interface OnBody {
        void onBody(String body);
    }

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

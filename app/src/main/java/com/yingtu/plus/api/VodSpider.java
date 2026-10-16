package com.yingtu.plus.api;

import com.yingtu.plus.bean.SourceBean;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spider 执行门面：把「拿到当前站点的 Spider 并在后台线程调用」这件事收口。
 *
 * CatVod 的 Spider（尤其 JAR 壳）首次初始化会下载 jar + 释放 native so，耗时且需后台线程；
 * 调用 homeContent/categoryContent/detailContent/searchContent/playerContent 也必须离开主线程。
 */
public class VodSpider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private VodSpider() {}

    public static ExecutorService executor() {
        return EXECUTOR;
    }

    /** 取某站点的 Spider（会按需加载 jar）。非主线程调用。 */
    public static Spider get(SourceBean site) {
        if (site == null) return new SpiderNull();
        try {
            return SpiderManager.get().getSpider(site, ApiConfig.get().getSpider());
        } catch (Throwable e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    /** 是否 CSP（type=3 / api 以 csp_ 开头）站点。 */
    public static boolean isCsp(SourceBean site) {
        return site != null && site.getType() == 3 && site.getApi() != null && site.getApi().startsWith("csp_");
    }
}

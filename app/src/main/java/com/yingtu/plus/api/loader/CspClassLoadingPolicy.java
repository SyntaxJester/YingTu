package com.yingtu.plus.com.yingtu.plus.api.loader;

final class CspClassLoadingPolicy {

    private static final String PROTOBUF_PREFIX = "com.google.protobuf.";

    private CspClassLoadingPolicy() {
    }

    static boolean isChildFirst(String name) {
        return name != null && name.startsWith(PROTOBUF_PREFIX);
    }
}

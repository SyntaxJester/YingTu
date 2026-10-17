package com.yingtu.plus.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link com.yingtu.plus.bean.Danmaku} 列表的反序列化器（webhtv 移植）。
 */
public class DanmakuAdapter implements JsonDeserializer<List> {

    @Override
    public List deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return parsePrimitive(json.getAsString().trim());
        }
        return context.deserialize(json, typeOfT);
    }

    @SuppressWarnings("unchecked")
    private List parsePrimitive(String text) {
        if (text.startsWith("[")) {
            return com.yingtu.plus.App.gson().fromJson(text, List.class);
        }
        List list = new ArrayList();
        list.add(com.yingtu.plus.bean.Danmaku.from(text));
        return list;
    }
}

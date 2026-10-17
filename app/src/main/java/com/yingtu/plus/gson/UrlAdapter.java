package com.yingtu.plus.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Url 列表反序列化器，支持单个 URL 字符串或数组。
 */
public class UrlAdapter implements JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonPrimitive()) {
            List<String> list = new ArrayList<>();
            list.add(json.getAsString());
            return list;
        }
        if (json.isJsonObject()) {
            return context.deserialize(json, typeOfT);
        }
        return context.deserialize(json, typeOfT);
    }
}

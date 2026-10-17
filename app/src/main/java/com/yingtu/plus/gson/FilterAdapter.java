package com.yingtu.plus.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter 反序列化器，将 JSON 数组转为 Map。
 */
public class FilterAdapter implements JsonDeserializer<Map<String, String>> {

    @Override
    public Map<String, String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (!json.isJsonArray()) {
            return new HashMap<>();
        }
        Map<String, String> map = new HashMap<>();
        for (JsonElement item : json.getAsJsonArray()) {
            if (item.isJsonObject()) {
                JsonObject obj = item.getAsJsonObject();
                String name = obj.has("n") ? obj.get("n").getAsString() : "";
                String value = obj.has("v") ? obj.get("v").getAsString() : "";
                map.put(name, value);
            }
        }
        return map;
    }
}

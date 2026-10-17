package com.yingtu.plus.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Header 反序列化器，支持字符串或 JSON 对象。
 */
public class HeaderAdapter implements JsonDeserializer<Map<String, String>> {

    @Override
    public Map<String, String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        }
        return parseHeaderString(json.getAsString());
    }

    private Map<String, String> parseHeaderString(String text) {
        Map<String, String> headers = new HashMap<>();
        for (String line : text.split("\\r?\\n")) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return headers;
    }
}

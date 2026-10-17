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
 * Msg 反序列化器（支持字符串或 JSON 对象）。
 */
public class MsgAdapter implements JsonDeserializer<Map<String, String>> {

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
        Map<String, String> result = new HashMap<>();
        result.put("text", json.getAsString());
        return result;
    }
}

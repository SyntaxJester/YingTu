package com.yingtu.plus.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Ext（扩展参数）反序列化器。
 */
public class ExtAdapter implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            return com.yingtu.plus.App.gson().toJson(json);
        }
        return json.getAsString();
    }
}

package com.snet.smore.transformer.converter;

import com.google.gson.JsonObject;

public class TestConverter implements SmoreConverter {
    @Override
    public JsonObject convert(byte[] bytes) {
        JsonObject json = new JsonObject();
        json.addProperty("test", "TestConverter");

        return json;
    }
}
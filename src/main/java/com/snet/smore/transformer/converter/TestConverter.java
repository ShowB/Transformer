package com.snet.smore.transformer.converter;


import org.json.simple.JSONObject;

public class TestConverter implements SmoreConverter {

    @Override
    public JSONObject convert(byte[] bytes) {
        JSONObject json = new JSONObject();
        json.put("test", "TestConverter");

        return json;
    }
}
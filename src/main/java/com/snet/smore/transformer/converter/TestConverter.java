package com.snet.smore.transformer.converter;


import org.json.simple.JSONObject;

import java.nio.file.Path;

public class TestConverter extends AbstractBinaryConverter {

    TestConverter(Path path) throws Exception {
        super(path);
    }

    @Override
    public JSONObject convertOneRow(byte[] bytes) {
        JSONObject json = new JSONObject();
        json.put("test", "TestConverter");

        return json;
    }
}
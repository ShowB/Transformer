package com.snet.smore.transformer.converter;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.file.Path;

public class TestConverter extends AbstractBinaryConverter {

    public TestConverter(Path path) throws Exception {
        super(path);
        setByteSize(2552);
    }

    @Override
    public JSONArray convertOneRow(byte[] bytes) {
        JSONArray array = new JSONArray();

        JSONObject json = new JSONObject();
        json.put("test", "TestConverter");

        array.add(json);
        return array;
    }

    @Override
    public void correct() {

    }
}
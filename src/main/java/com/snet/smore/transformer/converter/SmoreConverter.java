package com.snet.smore.transformer.converter;

import org.json.simple.JSONObject;

public interface SmoreConverter {
    JSONObject convert(byte[] bytes);
}

package com.snet.smore.transformer.converter;

import com.google.gson.JsonObject;

public interface SmoreConverter {
    JsonObject convert(byte[] bytes);
}

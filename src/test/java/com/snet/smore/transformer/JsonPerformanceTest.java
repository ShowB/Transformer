package com.snet.smore.transformer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonPerformanceTest {
    @Test
    @Ignore
    public void test() throws InterruptedException {
        JsonObject main = new JsonObject();
        JsonArray array = new JsonArray();
        JsonObject json = new JsonObject();

        for (int n = 0; n < 10; n++) {
            for (int i = 0; i < 3600; i++) {
                array = new JsonArray();
                json = new JsonObject();

                for (int j = 0; j < 6000; j++) {
                    json.addProperty("key" + j, "value" + j);

//                    if (j % 1000 == 0)
//                        System.out.println("[" + i + " , " + j + "]");
                }
                array.add(json);
            }

            main.add("Array" + n, array);
            System.out.println(n);
        }

        System.out.println(main);

    }

    @Test
    @Ignore
    public void test2() {
        Map<String, Object> main = new HashMap<>();
        List<Map<String, Object>> record = new ArrayList<>();
        Map<String, Object> field = new HashMap<>();

        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3600; i++) {
            record.clear();
            field.clear();
            for (int j = 0; j < 6000; j++) {
//                field.addProperty("key" + j, "value" + j);
                field.put("key" + j, "value" + j);
                record.add(field);

//                if (j % 1000 == 0)
//                    System.out.println("[" + i + " , " + j + "]");
            }
            main.put("Array" + i, record);
//            sb.append(record);
        }

//        System.out.println(sb.length());
        System.out.println(main.size());

    }

    @Test
    @Ignore
    public void test3() {
        JSONObject main = new JSONObject();
        JSONArray array = new JSONArray();
        JSONObject json = new JSONObject();

        for (int i = 0; i < 3600; i++) {
            array.clear();
            json.clear();
            for (int j = 0; j < 6000; j++) {
                json.put("key" + j, "value" + j);
                array.add(json);

                if (j % 1000 == 0)
                    System.out.println("[" + i + " , " + j + "]");
            }
            main.put("Array" + i, array);
        }

        System.out.println(main);
    }

    @Test
    @Ignore
    public void test4() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 3600; i++) {
            StringBuilder record = new StringBuilder();
            StringBuilder field = new StringBuilder();
            for (int j = 0; j < 6000; j++) {
                field.append("key").append(j).append("value").append(j);

                if (j % 1000 == 0)
                    System.out.println("[" + i + " , " + j + "]");
            }
            record.append(field).append("\n");
            sb.append(record);
        }

        System.out.println(sb);
    }

    @Test
    @Ignore
    public void test5() {
        JSONArray array = new JSONArray();
        JSONArray array2 = new JSONArray();
        JSONObject json = new JSONObject();

        json.put("a", "aaa");
        json.put("b", "bbb");

        array.add(json);
        array2.add(json);
        array.add(array2);



        System.out.println(array);

    }
}

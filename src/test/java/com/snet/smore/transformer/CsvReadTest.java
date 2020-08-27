package com.snet.smore.transformer;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;
import com.snet.smore.common.util.EnvManager;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class CsvReadTest {
    @Test
    @Ignore
    public void test() throws IOException {
        Path path = Paths.get("D:/total.csv");
        String tableName = EnvManager.getProperty("transformer.target.table-name");

        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);

        int fileSize = (int) Files.size(path);
        ByteBuffer buffer = ByteBuffer.allocateDirect(fileSize);
        byte[] bytes = new byte[fileSize];
        channel.read(buffer);

        buffer.flip();

        buffer.get(bytes);

        final FileReader fileReader = new FileReader(path.toFile());
        CSVReader csvReader = new CSVReader(fileReader);

        List<JSONObject> rows = new LinkedList<>();

        JSONObject row = new JSONObject();
        JSONObject main = new JSONObject();
        List<String> keys = new LinkedList<>();
        String[] line;
        int lineCnt = 1;

        while ((line = csvReader.readNext()) != null) {
            if (lineCnt == 1) {
                keys.addAll(Arrays.asList(line));
            } else {
                main.clear();
                row.clear();
                for (int i = 0; i < line.length; i++) {
                    row.put(keys.get(i), line[i]);
                }
                main.put(tableName, row.clone());
                rows.add((JSONObject) main.clone());
            }
            lineCnt++;
        }

        csvReader.close();
        fileReader.close();


        StringBuilder sb = new StringBuilder();
        for (JSONObject e : rows) {
            sb.append(e.toJSONString());
        }

        System.out.println(sb.length());
//        System.out.println(rows.size());
    }
}

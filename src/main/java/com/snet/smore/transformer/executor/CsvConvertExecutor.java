package com.snet.smore.transformer.executor;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class CsvConvertExecutor extends AbstractExecutor {

    public CsvConvertExecutor(Path path) {
        super(path);
    }

    @Override
    public String call() {
        int maxLine = EnvManager.getProperty("transformer.target.file.max-line", 10000);

        try {
            path = FileUtil.changeFileStatus(path, FileStatusPrefix.TEMP);
        } catch (IOException e) {
            log.error("File is using by another process. {}", path);
            return Constant.CALLABLE_RESULT_FAIL;
        }

        JSONObject row = new JSONObject();
        List<String> keys = new LinkedList<>();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             InputStreamReader isr = new InputStreamReader(fis, EnvManager.getProperty("transformer.source.file.encoding", "UTF-8"));
             CSVReader csvReader = new CSVReader(isr
                     , EnvManager.getProperty("transformer.source.file.csv.separator", ',')
                     , EnvManager.getProperty("transformer.source.file.csv.quote", CSVWriter.NO_QUOTE_CHARACTER))) {

            String[] line;
            int lineCnt = 0;

            changeNewFile();

            while ((line = csvReader.readNext()) != null) {
                if (lineCnt == 0) {
                    keys.addAll(Arrays.asList(line));
                } else {
                    row.clear();
                    for (int i = 0; i < line.length; i++) {
                        try {
                            row.put(keys.get(i), "NULL".equalsIgnoreCase(line[i]) ? null : line[i]);
                        } catch (Exception ex) {
                            log.error(ex.getMessage() + " {}", lineCnt);
                        }
                    }
                    targetFileChannel.write(ByteBuffer.wrap(row.toJSONString().getBytes()));
                    targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));
                }
                lineCnt++;

                if (lineCnt % maxLine == 0)
                    changeNewFile();

            }

            csvReader.close();
            isr.close();
            fis.close();

            closeChannel();
            closeFile();

            path = FileUtil.changeFileStatus(path, FileStatusPrefix.COMPLETE);

            log.info("Convert was successfully completed. [{}]\t[{} / {}]", originFileName, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());

            return Constant.CALLABLE_RESULT_SUCCESS;


        } catch (Exception e) {
            return error(e);
        }
    }
}

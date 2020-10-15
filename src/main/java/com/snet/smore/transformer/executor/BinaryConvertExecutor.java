package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.common.util.StringUtil;
import com.snet.smore.transformer.converter.AbstractBinaryConverter;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class BinaryConvertExecutor extends AbstractExecutor {

    public BinaryConvertExecutor(Path path) {
        super(path);
    }

    @Override
    public String call() {
        try {
            path = FileUtil.changeFileStatus(path, FileStatusPrefix.TEMP);
        } catch (IOException e) {
            log.error("File is using by another process. {}", path);
            return Constant.CALLABLE_RESULT_FAIL;
        }

        try {
            int maxLine = EnvManager.getProperty("transformer.target.file.max-line", 10000);

            List<Class> classes = getConvertClasses();

            if (classes.size() < 1) {
                log.error("Cannot convert value [transformer.binary.converter.fqcn].");
                return Constant.CALLABLE_RESULT_FAIL;
            }

            String targetRoot;
            String csvLine;
            int rowCnt = 0;
            JSONArray array;
            JSONObject json;
            AbstractBinaryConverter converter;

            for (Class clazz : classes) {
                converter = (AbstractBinaryConverter) clazz.getConstructor(Path.class).newInstance(path);

                targetRoot = EnvManager.getProperty("transformer.target.file.dir." + clazz.getName());
                changeNewFile(targetRoot);

                while (converter.hasNext()) {
                    array = converter.next();

                    if (array == null || array.size() == 0)
                        continue;

                    for (int i = 0; i < array.size(); i++) {
                        json = (JSONObject) array.get(i);

                        if (json == null || json.size() == 0)
                            continue;

                        if ("csv".equalsIgnoreCase(targetFileType)) {
                            if (rowCnt == 0) {
                                csvLine = generateCsvHeader(json);
                                targetFileChannel.write(ByteBuffer.wrap(csvLine.getBytes()));
                                targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));
                            }

                            if (rowCnt > 0)
                                targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));

                            csvLine = generateCsvLine(json);
                            targetFileChannel.write(ByteBuffer.wrap(csvLine.getBytes()));

                        } else if ("json".equalsIgnoreCase(targetFileType)) {
                            if (rowCnt > 0) {
                                targetFileChannel.write(ByteBuffer.wrap(",".getBytes()));
                                targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));
                            }

                            targetFileChannel.write(ByteBuffer.wrap(json.toJSONString().getBytes()));
                        }

                        if (++rowCnt == maxLine) {
                            rowCnt = 0;
                            changeNewFile(targetRoot);
                        }
                    }
                }

                converter.clearBuffer();

                rowCnt = 0;

                closeChannel();
                closeFile();

            }
            log.info("Convert was successfully completed. [{}]\t[{} / {}]", originFileName, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());

            try {
                path = FileUtil.changeFileStatus(path, FileStatusPrefix.COMPLETE);
            } catch (IOException e) {
                log.error("File is using by another process. {}", path);
                return Constant.CALLABLE_RESULT_FAIL;
            }

            return Constant.CALLABLE_RESULT_SUCCESS;

        } catch (Exception e) {
            return error(e);
        }
    }

    private String generateCsvHeader(JSONObject json) {
        Iterator<Map.Entry> it = json.entrySet().iterator();

        StringBuilder sb = new StringBuilder();

        while (it.hasNext()) {
            sb.append(it.next().getKey());

            if (it.hasNext())
                sb.append(EnvManager.getProperty("transformer.target.file.csv.separator", ","));
        }

        return sb.toString();
    }

    private String generateCsvLine(JSONObject json) {
        Iterator<Map.Entry> it = json.entrySet().iterator();

        StringBuilder sb = new StringBuilder();

        while (it.hasNext()) {
            sb.append(it.next().getValue());

            if (it.hasNext())
                sb.append(EnvManager.getProperty("transformer.target.file.csv.separator", ","));
        }

        return sb.toString();
    }

    private Class getConvertClass() throws Exception {
        String fqcn = EnvManager.getProperty("transformer.binary.converter.fqcn");
        return Class.forName(fqcn);
    }

    private List<Class> getConvertClasses() throws Exception {
        List<Class> classes = new ArrayList<>();
        String fqcnValue = EnvManager.getProperty("transformer.binary.converter.fqcn");

        if (StringUtil.isBlank(fqcnValue))
            return classes;

        String[] fqcns = EnvManager.getProperty("transformer.binary.converter.fqcn").split(";");

        for (String s : fqcns) {
            classes.add(Class.forName(s));
        }

        return classes;
    }
}

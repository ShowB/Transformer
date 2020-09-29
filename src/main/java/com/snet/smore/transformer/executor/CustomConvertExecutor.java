package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.common.util.StringUtil;
import com.snet.smore.transformer.converter.AbstractCustomConverter;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomConvertExecutor extends AbstractExecutor {

    public CustomConvertExecutor(Path path) {
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
            List<Class> classes = getConvertClasses();

            if (classes.size() < 1) {
                log.error("Cannot convert value [transformer.binary.converter.fqcn].");
                return Constant.CALLABLE_RESULT_FAIL;
            }

            String targetRoot;
            AbstractCustomConverter converter;

            for (Class clazz : classes) {
                converter = (AbstractCustomConverter) clazz.getConstructor(Path.class).newInstance(path);

                targetRoot = EnvManager.getProperty("transformer.target.file.dir." + clazz.getName());
                changeNewFile(targetRoot);

                converter.convert();

//                closeChannel();
//                closeFile();
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

package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.common.util.StringUtil;
import com.snet.smore.transformer.converter.AbstractCustomConverter;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
                converter.convert();
                converter.clearBuffer();
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

package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.common.util.StringUtil;
import com.snet.smore.transformer.converter.AbstractBinaryConverter;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

            for (Class clazz : classes) {
                AbstractBinaryConverter converter
                        = (AbstractBinaryConverter) clazz.getConstructor(Path.class).newInstance(path);

                int lineCnt = 0;
                JSONObject json;

                String targetRoot = EnvManager.getProperty("transformer.target.file.dir." + clazz.getName());
                changeNewFile(targetRoot);
                while ((json = converter.next()) != null) {
                    targetFileChannel.write(ByteBuffer.wrap(json.toJSONString().getBytes()));
                    targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));

                    if (++lineCnt == maxLine)
                        changeNewFile(targetRoot);
                }

                closeChannel();
                closeFile();

            }
            log.info("Convert was successfully completed. [{}]\t[{} / {}]", originFileName, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());

            return Constant.CALLABLE_RESULT_SUCCESS;

        } catch (Exception e) {
            return error(e);
        }
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

package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.transformer.converter.AbstractBinaryConverter;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

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

            AbstractBinaryConverter converter
                    = (AbstractBinaryConverter) getConvertClass().getConstructor(Path.class).newInstance(path);

            int lineCnt = 0;
            JSONObject json;

            changeNewFile();
            while ((json = converter.next()) != null) {
                System.out.println(++lineCnt);
                targetFileChannel.write(ByteBuffer.wrap(json.toJSONString().getBytes()));
                targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));

                if (lineCnt % maxLine == 0)
                    changeNewFile();
            }

            closeChannel();
            closeFile();

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
}

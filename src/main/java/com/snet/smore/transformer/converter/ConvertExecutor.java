package com.snet.smore.transformer.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.concurrent.Callable;

@Slf4j
public class ConvertExecutor implements Callable<String> {
    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";

    private Path path;
    private Method convertMethod;
    private int byteSize;

    public ConvertExecutor(Path path, Method convertMethod, int byteSize) {
        this.path = path;
        this.convertMethod = convertMethod;
        this.byteSize = byteSize;
    }

    @Override
    public String call() {
        Path originPath = path;

        try {
            path = FileUtil.changeFileStatus(path, FileStatusPrefix.TEMP);
        } catch (IOException e) {
            log.error("File is using by another process. {}", path);
            return FAIL;
        }

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) Files.size(path));
            channel.read(buffer);

            if (buffer.limit() < byteSize) {
                return FAIL;
            }

            buffer.flip();

            byte[] bytes = new byte[byteSize];

            JsonArray jsonArray = new JsonArray();
            JsonObject json = new JsonObject();
            while (buffer.position() < buffer.limit()) {
                buffer.get(bytes);
                json = (JsonObject) convertMethod.invoke(convertMethod.getDeclaringClass().newInstance(), bytes);
                jsonArray.add(json);
            }

            String targetRoot = EnvManager.getProperty("transformer.target.file.dir");
            Path targetPath = Paths.get(targetRoot, FileStatusPrefix.TEMP.getPrefix() + path.getFileName().toString());

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            FileChannel targetFileChannel = FileChannel.open(targetPath
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.WRITE
                    , StandardOpenOption.TRUNCATE_EXISTING);
            targetFileChannel.write(ByteBuffer.wrap(
                    gson.toJson(
                            jsonArray
                    ).getBytes()
                    )
            );

            targetPath = Files.move(targetPath
                    , Paths.get(targetRoot, originPath.getFileName().toString())
                    , StandardCopyOption.REPLACE_EXISTING);

            path = FileUtil.changeFileStatus(path, FileStatusPrefix.COMPLETE);

            log.info("Convert was successfully completed. {} --> {}, \t[{} / {}]", originPath, targetPath, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());
            return SUCCESS;

        } catch (Exception e) {
            log.error("An error occurred while converting file. [{}]", path, e);
            try {
                FileUtil.changeFileStatus(path, FileStatusPrefix.ERROR);
            } catch (IOException ex) {
                log.error("An error occurred while changing file name. [{}], {}", FileStatusPrefix.ERROR, path, e);
            }
            return FAIL;
        }
    }
}

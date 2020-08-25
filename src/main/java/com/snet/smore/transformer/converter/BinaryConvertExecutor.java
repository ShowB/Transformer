package com.snet.smore.transformer.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.Callable;

@Slf4j
public class BinaryConvertExecutor implements Callable<String> {
    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";

    private Path path;
    private Method convertMethod;
    private int byteSize;

    public BinaryConvertExecutor(Path path, Method convertMethod, int byteSize) {
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


            Object instance = convertMethod.getDeclaringClass().newInstance();

            String targetRoot = EnvManager.getProperty("transformer.target.file.dir");
            Files.createDirectories(Paths.get(targetRoot));

            Path targetPath = Paths.get(targetRoot, path.getFileName().toString());

            FileChannel targetFileChannel = FileChannel.open(targetPath
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.WRITE
                    , StandardOpenOption.TRUNCATE_EXISTING);

            int cursor = 0;
            int max = buffer.limit() / byteSize;
            JSONArray array = new JSONArray();
            JSONObject record;
            while (buffer.position() < buffer.limit()) {
                buffer.get(bytes);
                record = (JSONObject) convertMethod.invoke(instance, bytes);

//                array.add(record);
                targetFileChannel.write(ByteBuffer.wrap(record.toJSONString().getBytes()));

                if (++cursor < max)
                    targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));
            }

//            System.out.println(array.size());
//            targetFileChannel.write(ByteBuffer.wrap(array.toJSONString().getBytes()));

            targetFileChannel.close();

            long curr = System.currentTimeMillis();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            String targetFileName = originPath.getFileName().toString();

            int fileNameMaxLength = 13;
            int length = targetFileName.lastIndexOf(".");

            if (length == -1)
                length = targetFileName.length();

            length = Math.min(length, fileNameMaxLength);


            targetFileName = targetFileName.substring(0, length);
            targetFileName = Constant.sdf1.format(curr) + "_" + uuid + "_" + targetFileName + ".txt";



            targetPath = Files.move(targetPath
                    , Paths.get(targetRoot, targetFileName)
                    , StandardCopyOption.REPLACE_EXISTING);

            path = FileUtil.changeFileStatus(path, FileStatusPrefix.COMPLETE);

            log.info("Convert was successfully completed.\n{} --> {}\t[{} / {}]", originPath, targetPath, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());
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

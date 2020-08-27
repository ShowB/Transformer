package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.Callable;

@Slf4j
public abstract class AbstractExecutor implements Callable<String> {
    protected Path path;
    protected String originFileName;
    protected Path targetPath;
    protected FileChannel targetFileChannel;

    public AbstractExecutor(Path path) {
        this.path = path;
        this.originFileName = path.getFileName().toString();
    }

    @Override
    public abstract String call();

    protected void changeNewFile() {
        closeChannel();
        closeFile();

        String targetRoot = EnvManager.getProperty("transformer.target.file.dir");

        long curr = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String targetFileName = originFileName;

        int fileNameMaxLength = 13;
        int length = targetFileName.lastIndexOf(".");

        if (length == -1)
            length = targetFileName.length();

        length = Math.min(length, fileNameMaxLength);

        targetFileName = targetFileName.substring(0, length);
        targetFileName = FileStatusPrefix.TEMP.getPrefix() + Constant.sdf1.format(curr) + "_" + uuid + "_" + targetFileName + ".txt";

        targetPath = Paths.get(targetRoot, targetFileName);

        try {
            targetFileChannel = FileChannel.open(targetPath
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.WRITE
                    , StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("An error occurred while opening file channel.", e);
        }
    }

    protected void closeFile() {
        if (targetPath != null) {
            try {
                targetPath = FileUtil.changeFileStatus(targetPath, null);
                log.info("File was successfully created. [{}]", targetPath);
            } catch (IOException e) {
                log.error("An error occurred while changing file name. {}", targetPath, e);
            }
        }
    }

    protected void closeChannel() {
        if (targetFileChannel != null) {
            try {
                targetFileChannel.close();
            } catch (IOException e) {
                log.error("An error occurred while closing file channel.", e);
            }
        }
    }

    protected String error(Exception e) {
        log.error("An error occurred while converting file. {}", path, e);
        try {
            FileUtil.changeFileStatus(path, FileStatusPrefix.ERROR);
            Files.delete(targetPath);
        } catch (Exception ex) {
            log.error("An error occurred while changing file name. {}", path, ex);
        }
        return Constant.CALLABLE_RESULT_FAIL;
    }
}

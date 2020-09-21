package com.snet.smore.transformer.executor;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
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
    protected String targetFileType;
    private String targetFileExt;

    public AbstractExecutor(Path path) {
        this.path = path;
        this.originFileName = path.getFileName().toString();
        this.targetFileType = EnvManager.getProperty("transformer.target.file.type", "json");
        this.targetFileExt = EnvManager.getProperty("transformer.target.file.ext");
        if (!this.targetFileExt.startsWith(".")) {
            targetFileExt = "." + targetFileExt;
        }
    }

    @Override
    public abstract String call();

    protected void changeNewFile() {
        changeNewFileProcess(EnvManager.getProperty("transformer.target.file.dir"));
    }

    protected void changeNewFile(String targetRoot) {
        if (StringUtil.isBlank(targetRoot))
            changeNewFile();
        else
            changeNewFileProcess(targetRoot);
    }

    private void changeNewFileProcess(String targetRoot) {
        try {
            Files.createDirectories(Paths.get(targetRoot));
        } catch (Exception e) {
            log.error("An error while creating directory. [{}]", targetRoot, e);
        }

        closeChannel();
        closeFile();

        long curr = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String targetFileName = originFileName;

        int fileNameMaxLength = 13;
        int length = targetFileName.lastIndexOf(".");

        if (length == -1)
            length = targetFileName.length();

        length = Math.min(length, fileNameMaxLength);

        targetFileName = targetFileName.substring(0, length);
        targetFileName = FileStatusPrefix.TEMP.getPrefix() + curr + "_" + uuid + "_" + targetFileName + targetFileExt;

        targetPath = Paths.get(targetRoot, targetFileName);

        try {
            targetFileChannel = FileChannel.open(targetPath
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.WRITE
                    , StandardOpenOption.TRUNCATE_EXISTING);

            if ("json".equalsIgnoreCase(targetFileType))
                targetFileChannel.write(ByteBuffer.wrap("[".getBytes()));
        } catch (IOException e) {
            error(e);
        }
    }

    protected void closeFile() {
        if (targetPath != null && Files.isRegularFile(targetPath)) {
            try {
                targetPath = FileUtil.changeFileStatus(targetPath, null);
                log.info("File was successfully created. [{}]", targetPath);
            } catch (IOException e) {
                log.error("An error occurred while changing file name. {}", targetPath, e);
            }
        }

        targetPath = null;
    }

    protected void closeChannel() {
        if (targetFileChannel != null && targetFileChannel.isOpen()) {
            try {
                if ("json".equalsIgnoreCase(targetFileType)) {
                    if (targetFileChannel.size() > 1)
                        targetFileChannel.write(ByteBuffer.wrap("]".getBytes()));
                    else
                        Files.delete(targetPath);

                } else {
                    if (targetFileChannel.size() < 1)
                        Files.delete(targetPath);
                }

                targetFileChannel.close();
            } catch (IOException e) {
                log.error("An error occurred while closing file channel.", e);
            }
        }

        targetFileChannel = null;
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

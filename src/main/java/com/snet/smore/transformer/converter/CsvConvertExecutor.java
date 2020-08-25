package com.snet.smore.transformer.converter;

import au.com.bytecode.opencsv.CSVReader;
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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

@Slf4j
public class CsvConvertExecutor implements Callable<String> {
    private Path path;
    private Path targetPath;
    private FileChannel targetFileChannel;

    public CsvConvertExecutor(Path path) {
        this.path = path;
    }

    @Override
    public String call() {
        Path originPath = path;
        int maxLine = EnvManager.getProperty("transformer.target.file.max-line", 10000);

        try {
            path = FileUtil.changeFileStatus(path, FileStatusPrefix.TEMP);
        } catch (IOException e) {
            log.error("File is using by another process. {}", path);
            return Constant.CALLABLE_RESULT_FAIL;
        }

        String tableName = EnvManager.getProperty("transformer.target.table-name", "TABLE_NAME");

        JSONObject row = new JSONObject();
        JSONObject main = new JSONObject();
        List<String> keys = new LinkedList<>();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             InputStreamReader isr = new InputStreamReader(fis, "euc-kr");
             CSVReader csvReader = new CSVReader(isr)) {

            String[] line;
            int lineCnt = 0;

            changeNewFile(originPath);

            while ((line = csvReader.readNext()) != null) {
                if (lineCnt == 0) {
                    keys.addAll(Arrays.asList(line));
                } else {
                    main.clear();
                    row.clear();
                    for (int i = 0; i < line.length; i++) {
                        row.put(keys.get(i), line[i]);
                    }
                    main.put(tableName, row.clone());
                    targetFileChannel.write(ByteBuffer.wrap(main.toJSONString().getBytes()));
                    targetFileChannel.write(ByteBuffer.wrap(Constant.LINE_SEPARATOR.getBytes()));
                }
                lineCnt++;

                if (lineCnt % maxLine == 0)
                    changeNewFile(originPath);

            }

            csvReader.close();
            isr.close();
            fis.close();
            closeFile();

            path = FileUtil.changeFileStatus(path, FileStatusPrefix.COMPLETE);

            log.info("Convert was successfully completed. [{}]\t[{} / {}]", originPath, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());

            return Constant.CALLABLE_RESULT_SUCCESS;


        } catch (Exception e) {
            log.error("An error occurred while converting file. {}", path, e);
            try {
                FileUtil.changeFileStatus(path, FileStatusPrefix.ERROR);
                Files.delete(targetPath);
            } catch (Exception ex) {
                log.error("An error occurred while changing file name. {}", path, ex);
            }
            return Constant.CALLABLE_RESULT_SUCCESS;
        }
    }

    private void changeNewFile(Path originPath) {
        closeFile();
        closeChannel();

        String targetRoot = EnvManager.getProperty("transformer.target.file.dir");

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

        targetPath = Paths.get(targetRoot, FileStatusPrefix.TEMP.getPrefix() + targetFileName);

        try {
            targetFileChannel = FileChannel.open(targetPath
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.WRITE
                    , StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("An error occurred while opening file channel.", e);
        }
    }

    private void closeFile() {
        if (targetPath != null) {
            try {
                FileUtil.changeFileStatus(targetPath, null);
                log.info("File was successfully created. [{}]", targetPath);
            } catch (IOException e) {
                log.error("An error occurred while changing file name. {}", targetPath, e);
            }
        }
    }

    private void closeChannel() {
        if (targetFileChannel != null) {
            try {
                targetFileChannel.close();
            } catch (IOException e) {
                log.error("An error occurred while closing file channel.", e);
            }
        }
    }
}

package com.snet.smore.transformer.module;

import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.transformer.converter.CsvConvertExecutor;
import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class CsvConvertModule {
    public static void execute() {
        int threadCnt = EnvManager.getProperty("transformer.thread.count", 10);

        if (threadCnt < 1) {
            log.error("Cannot convert value [transformer.thread.count]. " +
                    "Job will be restarted.");
            return;
        }

        TransformerMain.setTotalCnt(0);
        TransformerMain.clearCurrCnt();

        Path root = Paths.get(EnvManager.getProperty("transformer.source.file.dir"));
        String source = EnvManager.getProperty("transformer.source.file.glob");
        List<Path> files = FileUtil.findFiles(root, source);

        if (files.size() < 1)
            return;

        TransformerMain.setTotalCnt(files.size());
        log.info("Target files were found: {}", TransformerMain.getTotalCnt());
        long start = System.currentTimeMillis();

        ExecutorService distributeService = Executors.newFixedThreadPool(threadCnt);
        List<Callable<String>> callables = new ArrayList<>();

        for (Path p : files) {
            callables.add(new CsvConvertExecutor(p));
        }

        try {
            List<Future<String>> futures = distributeService.invokeAll(callables);
            long end = System.currentTimeMillis();
            log.info("{} files convert have been completed.", futures.size());
            log.info("Turn Around Time: " + ((end - start) / 1000) + " (seconds)");
        } catch (InterruptedException e) {
            log.error("An error occurred while invoking distributed thread.");
        }
    }
}

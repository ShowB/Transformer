package com.snet.smore.transformer.module;

import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.transformer.executor.CustomConvertExecutor;
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
public class CustomConvertModule {
    public void execute() {
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

        ExecutorService distributeService = Executors.newFixedThreadPool(1);
        List<Callable<String>> callables = new ArrayList<>();

        for (Path p : files) {
            callables.add(new CustomConvertExecutor(p));
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

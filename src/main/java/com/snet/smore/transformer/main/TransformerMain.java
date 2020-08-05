package com.snet.smore.transformer.main;

import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import com.snet.smore.common.util.StringUtil;
import com.snet.smore.transformer.converter.ConvertExecutor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TransformerMain {
    private static boolean isRunning = true;
    private static boolean isPrevRunning = false;
    private static Integer totalCnt = 0;
    private static Integer currCnt = 0;

    private static int byteSize = 0;
    private static int threadCntDefault = 50;
    private static int threadCnt = 0;

    public static Integer getTotalCnt() {
        return totalCnt;
    }

    private static void setTotalCnt(Integer totalCnt) {
        TransformerMain.totalCnt = totalCnt;
    }

    public static Integer getNextCnt() {
        synchronized (currCnt) {
            return ++currCnt;
        }
    }

    private static void clearCurrCnt() {
        synchronized (currCnt) {
            currCnt = 0;
        }
    }

    public static void main(String[] args) {
        ScheduledExecutorService mainService = Executors.newSingleThreadScheduledExecutor();

        Runnable runnable = () -> {
            if (!isRunning) {
                isPrevRunning = false;
                return;
            }

            if (!isPrevRunning) {
                EnvManager.reload();

                try {
                    byteSize = Integer.parseInt(EnvManager.getProperty("transformer.source.byte.size"));
                } catch (Exception e) {
                    log.info("Cannot convert value [transformer.source.byte.size]. " +
                            "Job will be restarted.");
                    return;
                }

                if (byteSize < 1) {
                    log.info("Cannot convert value [transformer.source.byte.size]. " +
                            "Job will be restarted.");
                    return;
                }

                try {
                    threadCnt = Integer.parseInt(EnvManager.getProperty("transformer.thread.count"));
                } catch (Exception e) {
                    log.info("Cannot convert value [transformer.thread.count]. " +
                            "System will be set default value: {}", threadCntDefault);
                    threadCnt = threadCntDefault;
                }

                if (threadCnt < 1) {
                    log.info("Cannot convert value [transformer.thread.count]. " +
                            "Job will be restarted.");
                    return;
                }

                loadConverter();

                isPrevRunning = true;
            }

            setTotalCnt(0);
            clearCurrCnt();

            Path root = Paths.get(EnvManager.getProperty("transformer.source.file.dir"));
            String source = EnvManager.getProperty("transformer.source.file.glob");
            List<Path> files = FileUtil.findFiles(root, source);

            if (files.size() < 1)
                return;

            setTotalCnt(files.size());
            log.info("Target files were found: {}", getTotalCnt());
            long start = System.currentTimeMillis();

            Method convertMethod = getConvertMethod();

            ExecutorService distributeService = Executors.newFixedThreadPool(threadCnt);
            List<Callable<String>> callables = new ArrayList<>();

            for (Path p : files) {
                callables.add(new ConvertExecutor(p, convertMethod, byteSize));
            }

            try {
                List<Future<String>> futures = distributeService.invokeAll(callables);
                long end = System.currentTimeMillis();
                log.info("{} files convert have been completed.", futures.size());
                log.info("Turn Around Time: " + ((end - start) / 1000) + " (seconds)");
            } catch (InterruptedException e) {
                log.error("An error occurred while invoking distributed thread.");
            }
        };

        mainService.scheduleWithFixedDelay(runnable, 5, 1, TimeUnit.SECONDS);
    }

    private static Method getConvertMethod() {
        Method convertMethod = null;

        try {
            String fqcn = EnvManager.getProperty("transformer.converter.fqcn");

            try {
                Class clazz = Class.forName(fqcn);
                convertMethod = clazz.getDeclaredMethod("convert", byte[].class);
//                convertMethod.invoke(clazz.newInstance());

            } catch (Exception e) {
                log.error("An error occurred while calling converter. [{}]", fqcn);
            }

        } catch (Exception e) {
            log.error("Unknown error occurred. Thread will be restarted.", e);
        }

        return convertMethod;
    }


    private static void loadConverter() {
        File root = new File("converter");
        File[] files = root.listFiles();

        if (files == null || files.length < 1)
            return;

        for (File f : files) {
            if (!f.getName().toUpperCase().endsWith(".JAR"))
                continue;

            try {
                URL url = f.toURI().toURL();

                URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                Method methodLoader = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                methodLoader.setAccessible(true);
                methodLoader.invoke(classLoader, url);

                log.info("Converter was successfully loaded. [{}]", f);
            } catch (Exception e) {
                log.error("An error occurred while loading converters. [{}]", f);
            }
        }
    }
}

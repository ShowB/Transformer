package com.snet.smore.transformer.main;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.domain.Agent;
import com.snet.smore.common.util.AgentUtil;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.transformer.module.BinaryConvertModule;
import com.snet.smore.transformer.module.CsvConvertModule;
import com.snet.smore.transformer.module.JsonConvertModule;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TransformerMain {
    private static String agentType = Constant.AGENT_TYPE_TRANSFORMER;
    private static String agentName = EnvManager.getProperty("transformer.name");

    private static boolean isRequiredPropertiesUpdate = true;
    private static Integer totalCnt = 0;
    private static Integer currCnt = 0;

    private static ScheduledExecutorService mainService = Executors.newSingleThreadScheduledExecutor();

    public static Integer getTotalCnt() {
        return totalCnt;
    }

    public static void setTotalCnt(Integer totalCnt) {
        TransformerMain.totalCnt = totalCnt;
    }

    public static Integer getNextCnt() {
        synchronized (currCnt) {
            return ++currCnt;
        }
    }

    public static void clearCurrCnt() {
        synchronized (currCnt) {
            currCnt = 0;
        }
    }

    public static void main(String[] args) {
        mainService.scheduleWithFixedDelay(TransformerMain::runAgent, 1, 1, TimeUnit.SECONDS);
    }

    private static void runAgent() {
        final Agent agent = AgentUtil.getAgent(agentType, agentName);

        if (!Constant.YN_Y.equalsIgnoreCase(agent.getUseYn()))
            return;

        isRequiredPropertiesUpdate = Constant.YN_Y.equalsIgnoreCase(agent.getChangeYn());

        if (isRequiredPropertiesUpdate) {
            EnvManager.reload();
            agentName = EnvManager.getProperty("transformer.name");

            log.info("Environment has successfully reloaded.");

            AgentUtil.setChangeYn(agentType, agentName, Constant.YN_N);
            isRequiredPropertiesUpdate = false;
        }

        TransformerMain.clearCurrCnt();

        final String type = EnvManager.getProperty("transformer.source.file.type");

        if ("bin".equalsIgnoreCase(type))
            BinaryConvertModule.execute();
        else if ("csv".equalsIgnoreCase(type))
            CsvConvertModule.execute();
        else if ("json".equalsIgnoreCase(type))
            JsonConvertModule.execute();
        else
            log.error("Cannot convert value [{}]. Thread will be restarted.", "transformer.source.file.type");
    }

    public static Method getConvertMethod() {
        Method convertMethod = null;

        try {
            String fqcn = EnvManager.getProperty("transformer.converter.fqcn");

            try {
                Class clazz = Class.forName(fqcn);
                convertMethod = clazz.getDeclaredMethod("convert", byte[].class);

            } catch (Exception e) {
                log.error("An error occurred while calling converter. [{}]", fqcn);
            }

        } catch (Exception e) {
            log.error("Unknown error occurred. Thread will be restarted.", e);
        }

        return convertMethod;
    }

    public static void loadConverter() {
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

package com.snet.smore.transformer.main;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.domain.Agent;
import com.snet.smore.common.util.AgentUtil;
import com.snet.smore.common.util.EnvManager;
import com.snet.smore.transformer.module.BinaryConvertModule;
import com.snet.smore.transformer.module.CsvConvertModule;
import com.snet.smore.transformer.module.JsonConvertModule;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TransformerMain {
    private static String agentType = Constant.AGENT_TYPE_TRANSFORMER;
    private static String agentName = EnvManager.getProperty("transformer.name");

    private static boolean isRequiredPropertiesUpdate = true;
    private static boolean isFirstRun = true;
    private static Integer totalCnt = 0;
    private static Integer currCnt = 0;

    private static ScheduledExecutorService mainService = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledExecutorService fileCompressService = Executors.newSingleThreadScheduledExecutor();

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
        fileCompressService.scheduleWithFixedDelay(TransformerMain::runFileCompress, 60, EnvManager.getProperty("transformer.backup.interval", 86400), TimeUnit.SECONDS);
    }

    private static void runAgent() {
        try {
            final Agent agent = AgentUtil.getAgent(agentType, agentName);

            if (!Constant.YN_Y.equalsIgnoreCase(agent.getUseYn()))
                return;

            isRequiredPropertiesUpdate = Constant.YN_Y.equalsIgnoreCase(agent.getChangeYn());

            if (isRequiredPropertiesUpdate || isFirstRun) {
                EnvManager.reload();
                agentName = EnvManager.getProperty("transformer.name");
                loadConverter();

                log.info("Environment has successfully reloaded.");

                if ("Y".equalsIgnoreCase(agent.getChangeYn()))
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

            if (isFirstRun)
                isFirstRun = false;

        } catch (Exception e) {
            log.error("An error occurred while thread processing. It will be restarted : {}", e.getMessage());
        }
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

    private static void runFileCompress() {
        try {
            Path sourceRoot = Paths.get(EnvManager.getProperty("transformer.source.file.dir"));
            Path targetRoot = Paths.get(EnvManager.getProperty("transformer.backup.file.dir"));

            List<Path> targets = findCmplFiles(sourceRoot);

            if (targets.size() < 1)
                return;

            String now = Constant.sdf1.format(System.currentTimeMillis());
            String zipPath = targetRoot.toAbsolutePath().toString();
            Path zipRoot = Paths.get(zipPath);
            Files.createDirectories(zipRoot);

            Map<String, String> zipProp = new HashMap<>();
            zipProp.put("create", Boolean.toString(true));
            zipProp.put("encoding", Charset.defaultCharset().displayName());

            URI zipFileUri = URI.create("jar:file:/" + zipPath.replaceAll("\\\\", "/") + "/" + now + ".zip");

            try (FileSystem zipfs = FileSystems.newFileSystem(zipFileUri, zipProp)) {

                int count = 0;
                Path temp;
                StringBuilder path = new StringBuilder();
                for (Path targetFile : targets) {
                    path.setLength(0);
                    path.append("/");

                    temp = targetFile;

                    if (!temp.getParent().equals(sourceRoot)) {
                        while (!temp.getParent().equals(sourceRoot)) {
                            path.insert(0, "/" + temp.getParent().getFileName().toString());
                            temp = temp.getParent();
                        }
                    }

                    Path targetPath = zipfs.getPath(path.toString(), targetFile.getFileName().toString());
                    Files.createDirectories(targetPath);
                    Files.move(targetFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                }

                if (count > 0)
                    log.info("{} files have successfully compressed. [{}]", count, zipFileUri);

            } catch (Exception e) {
                log.error("An error occurred while compress files.", e);
            }

        } catch (Exception e) {
            log.error("An error occurred while thread processing. It will be restarted : {}", e.getMessage());
        }
    }

    private static List<Path> findCmplFiles(Path root) {
        List<Path> files = new ArrayList<>();

        String glob = EnvManager.getProperty("transformer.source.file.glob", "*.*");
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

        try (Stream<Path> pathStream = Files.find(root, Integer.MAX_VALUE,
                (p, a) -> matcher.matches(p.getFileName())
                        && p.getFileName().toString().startsWith(FileStatusPrefix.COMPLETE.getPrefix())
                        && !a.isDirectory()
                        && a.isRegularFile())) {
            files = pathStream.collect(Collectors.toList());
        } catch (Exception e) {
            log.error("An error occurred while finding source files: {}", e.getMessage());
        }

        return files;
    }

}

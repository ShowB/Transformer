package com.snet.smore.transformer;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.util.EnvManager;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileCompressTest {
    @Test
    @Ignore
    public void test() throws IOException {
        Path root = Paths.get(EnvManager.getProperty("transformer.target.file.dir"));
        //압축파일을 배치할 경로 생성
        final String zipPath = root.toAbsolutePath().toString() + Constant.FILE_SEPARATOR + "backup";
        final Path zipRoot = Paths.get(zipPath);
        Files.createDirectories(zipRoot);

        //압축 파일 생성 옵션(새로생성, 파일명 캐릭터셋등)
        final Map<String, String> zipProp = new HashMap<>();
        zipProp.put("create", Boolean.toString(true));
        zipProp.put("encoding", Charset.defaultCharset().displayName());

        //압축파일 경로
        URI zipFileUri = URI.create("jar:file:/" + zipPath.replaceAll("\\\\", "/") + "/zipfile.zip");

        //압축파일 파일 시스템생성
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFileUri, zipProp)) {
            List<Path> targets = Files.list(root).filter(i -> !Files.isDirectory(i)).collect(Collectors.toList());
//            List<Path> targets = Files.find(root, Integer.MAX_VALUE, (p, a) -> !a.isDirectory() && !p.getParent().equals(zipRoot))
//                    .collect(Collectors.toList());

            for (Path targetFile : targets) {
                Path targetPath = zipfs.getPath("/", targetFile.getFileName().toString());
                Files.createDirectories(targetPath);

                //압축 파일안에 압축할 파일 넣기
                Files.move(targetFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void test2() {
        Path path = Paths.get(EnvManager.getProperty("transformer.target.file.dir"));
        String a = path.toAbsolutePath() + Constant.FILE_SEPARATOR + "back";

        System.out.println(a.replaceAll("\\\\", "/"));
    }
}

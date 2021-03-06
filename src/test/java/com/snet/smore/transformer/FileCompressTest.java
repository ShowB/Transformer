package com.snet.smore.transformer;

import com.snet.smore.common.constant.Constant;
import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.EnvManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FileCompressTest {
    @Test
    @Ignore
    public void test() throws IOException {
        Path sourceRoot = Paths.get(EnvManager.getProperty("transformer.source.file.dir"));
        Path targetRoot = Paths.get(EnvManager.getProperty("transformer.backup.file.dir"));

        //압축파일을 배치할 경로 생성
        String now = Constant.sdf1.format(System.currentTimeMillis());
        String zipPath = targetRoot.toAbsolutePath().toString();
        Path zipRoot = Paths.get(zipPath);
        Files.createDirectories(zipRoot);

        //압축 파일 생성 옵션(새로생성, 파일명 캐릭터셋등)
        Map<String, String> zipProp = new HashMap<>();
        zipProp.put("create", Boolean.toString(true));
        zipProp.put("encoding", Charset.defaultCharset().displayName());

        //압축파일 경로
        URI zipFileUri = URI.create("jar:file:/" + zipPath.replaceAll("\\\\", "/") + "/" + now + ".zip");

        //압축파일 파일 시스템생성
        try (FileSystem zipfs = FileSystems.newFileSystem(zipFileUri, zipProp)) {
            List<Path> targets = findCmplFiles(sourceRoot);

            int i = 0;
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

                //압축 파일안에 압축할 파일 넣기
                Files.move(targetFile, targetPath, StandardCopyOption.REPLACE_EXISTING);

                log.info("File is compressing ... [{} / {}]", ++i, targets.size());
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

    public List<Path> findCmplFiles(Path root) {
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

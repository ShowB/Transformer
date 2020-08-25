package com.snet.smore.transformer;

import com.snet.smore.common.constant.FileStatusPrefix;
import com.snet.smore.common.util.FileUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileCreateTest {
    @Test
    public void test() throws IOException {
        Path p = Paths.get("D:/", "sample.file");
        System.out.println(p);

        p = FileUtil.changeFileStatus(p, FileStatusPrefix.TEMP);

        FileChannel targetFileChannel = FileChannel.open(p
                , StandardOpenOption.CREATE
                , StandardOpenOption.WRITE
                , StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println(p);
    }
}

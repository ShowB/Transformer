package com.snet.smore.transformer;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileNameTest {
    @Test
    public void test() {
        Path path = Paths.get("D:\\SMORE_DATA\\LOADER_SOURCE\\TC_KORAIL_C0\\cmpl_1599449842872_61ccc22b_sample_7ced11.txt");
        System.out.println(path.getFileName());
    }
}

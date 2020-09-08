package com.snet.smore.transformer;

import com.snet.smore.transformer.converter.TestConverter;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileNameTest {
    @Test
    public void test() {
        Path path = Paths.get("D:\\SMORE_DATA\\LOADER_SOURCE\\TC_KORAIL_C0\\cmpl_1599449842872_61ccc22b_sample_7ced11.txt");
        System.out.println(path.getFileName());
    }

    @Test
    @Ignore
    public void test2() throws Exception {
        TestConverter con = new TestConverter(Paths.get("D:\\SMORE_DATA_BAK\\BAK\\1599203479179_a977ba66.bin"));
        System.out.println(con);
        System.out.println(con.hasNext());
        System.out.println(con.next());
    }
}

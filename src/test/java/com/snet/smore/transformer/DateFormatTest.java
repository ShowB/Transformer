package com.snet.smore.transformer;

import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.UUID;

public class DateFormatTest {
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");

    @Test
    public void test() {
        long curr = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String targetFileName = "sample_95a05ca4-73c0-4249-8fd2-5e2075f8d797.bin";

        int maxLength = 13;
        int length = targetFileName.lastIndexOf(".");

        if (length == -1)
            length = targetFileName.length();

        length = Math.min(length, maxLength);


        targetFileName = targetFileName.substring(0, length);


        System.out.println(curr + "_" + uuid + "_" + targetFileName + ".txt");
        System.out.println((curr + "_" + uuid + "_" + targetFileName + ".txt").length());
        System.out.println(sdf.format(curr) + "_" + uuid + "_" + targetFileName + ".txt");
        System.out.println((sdf.format(curr) + "_" + uuid + "_" + targetFileName + ".txt").length());

    }

    @Test
    @Ignore
    public void test2() {
        String prev = sdf.format(System.currentTimeMillis()) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String curr;
        long cnt = 0;
//        for (int i = 0; i < 10000000; i++) {
        for (; ; ) {
            curr = sdf.format(System.currentTimeMillis()) + "_" + UUID.randomUUID().toString().substring(0, 8);

            if (prev.equals(curr)) {
                System.out.println(prev + " : " + curr);
//                break;
            }

            cnt++;

            if (cnt == Long.MAX_VALUE) {
                System.out.println("No Dup !!!");
                break;
            }

            if (cnt % 10000000 == 0)
                System.out.println(cnt);

            prev = curr;
        }

        System.out.println(cnt);
    }

    @Test
    @Ignore
    public void test3() {
        System.out.println(System.currentTimeMillis() + "");
        System.out.println(sdf.format(System.currentTimeMillis()));
    }
}

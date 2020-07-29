package com.snet.smore.transformer;

import com.snet.smore.transformer.main.TransformerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class ByteBufferTest {
    Path path = Paths.get("D:\\smore\\TRANSFORMER_SOURCE\\test\\sample_0e3341db-0968-4084-9ae9-1d59d8059cc2.bin");
    int byteSize = 2552;

    @Test
    public void test() {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) Files.size(path));
            channel.read(buffer);

            buffer.flip();

            byte[] bytes = new byte[byteSize];

            while (buffer.position() < buffer.limit()) {
                buffer.get(bytes);
                System.out.println(bytes + "\tremaining bytes: " + (buffer.limit() - buffer.position()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test2() {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) Files.size(path));
            channel.read(buffer);

            buffer.flip();

            byte[] bytes = new byte[byteSize];

            while (buffer.position() < buffer.limit()) {
                buffer.get(bytes);
                System.out.println(bytes + "\tremaining bytes: " + (buffer.limit() - buffer.position()));

//                JsonObject json = (JsonObject) convertMethod.invoke(convertMethod.getDeclaringClass().newInstance(), bytes);

            }

            log.info("Convert was successfully completed. {} --> {}, \t[{} / {}]", path, path, TransformerMain.getNextCnt(), TransformerMain.getTotalCnt());


        } catch (Exception e) {
            log.error("An error occurred while converting file. [{}]", path, e);
        }
    }
}

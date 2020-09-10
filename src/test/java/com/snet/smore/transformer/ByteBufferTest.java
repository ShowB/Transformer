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
import java.util.Arrays;
import java.util.Random;

@Slf4j
public class ByteBufferTest {
    Path path = Paths.get("D:\\SMORE_DATA\\TRANSFORMER_SOURCE\\ROTEM\\1599628214696_30aa5e1d.bin");
    int byteSize = 1408;

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

    @Test
    public void test3() {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

            byte[] bytes = new byte[byteSize];
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) Files.size(path));
            channel.read(buffer);

            buffer.flip();

            buffer.get(bytes);
            System.out.println(Arrays.toString(bytes));

            byte[] addBytes = new byte[5];
            Random random = new Random();
            random.nextBytes(addBytes);

            int newSize = buffer.limit() + addBytes.length;
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() + addBytes.length);

            byte[] prevFile = new byte[buffer.capacity()];
            buffer.rewind();
            buffer.get(prevFile);

            newBuffer.put(addBytes);
            newBuffer.put(prevFile);
            newBuffer.flip();

            newBuffer.get(bytes);

            System.out.println(Arrays.toString(bytes));

        } catch (Exception e) {
            log.error("An error occurred while converting file. [{}]", path, e);
        }
    }
}

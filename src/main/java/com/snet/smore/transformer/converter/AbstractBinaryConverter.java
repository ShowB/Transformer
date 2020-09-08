package com.snet.smore.transformer.converter;

import com.snet.smore.common.util.EnvManager;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public abstract class AbstractBinaryConverter {
    private Path path;
    private FileChannel channel;
    private ByteBuffer buffer;
    private int byteSize;
    private byte[] bytes;

    public AbstractBinaryConverter(Path path) throws Exception {
        this.path = path;

//        byteSize = EnvManager.getProperty("transformer.source.byte.size", -1);

//        if (byteSize < 1) {
//            log.error("Cannot convert value [transformer.source.byte.size]. ");
//            throw new Exception();
//        }

        channel = FileChannel.open(this.path, StandardOpenOption.READ);
        buffer = ByteBuffer.allocateDirect((int) Files.size(path));
        channel.read(buffer);
        buffer.flip();
        channel.close();

        bytes = new byte[this.byteSize];
    }

    public abstract JSONArray convertOneRow(byte[] bytes);

    public boolean hasNext() {
        if (byteSize == 0) {
            log.error("Must set byteSize in Constructor of Converter via method: setByteSize().");
            return false;
        }

        return buffer.position() + byteSize <= buffer.limit();
    }

    public JSONArray next() {
        if (byteSize == 0) {
            log.error("Must set byteSize in Constructor of Converter via method: setByteSize().");
            return null;
        }

        if (buffer.limit() < byteSize) {
            return null;
        }


        if (buffer.position() + byteSize <= buffer.limit()) {
            buffer.get(bytes);
            return convertOneRow(bytes);
        } else {
            return null;
        }

    }

    protected void setByteSize(int byteSize) {
        this.byteSize = byteSize;
        this.bytes = new byte[byteSize];
    }

    protected int getByteSize() {
        return this.byteSize;
    }

    public Path getPath() {
        return this.path;
    }
}

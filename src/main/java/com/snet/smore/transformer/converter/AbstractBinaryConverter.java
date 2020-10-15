package com.snet.smore.transformer.converter;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public abstract class AbstractBinaryConverter {
    private Path path;
    private FileChannel channel;
    protected ByteBuffer buffer;
    private int byteSize;
    protected byte[] bytes;
    protected byte[] remainedBytes;

    public AbstractBinaryConverter(Path path) throws Exception {
        this.path = path;

        channel = FileChannel.open(this.path, StandardOpenOption.READ);
        buffer = ByteBuffer.allocateDirect((int) Files.size(path));
        channel.read(buffer);
        buffer.flip();
        channel.close();

        bytes = new byte[this.byteSize];
    }

    public abstract JSONArray convertOneRow(byte[] bytes);

    public abstract void correct();

    public boolean hasNext() {
        if (byteSize == 0) {
            log.error("Must set byteSize in Constructor of Converter via method: setByteSize().");
            return false;
        }

        correct();
        return buffer.position() + byteSize <= buffer.limit();
    }

    public JSONArray next() {
        if (byteSize == 0) {
            log.error("Must set byteSize in Constructor of Converter via method: setByteSize().");
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

    public void clearBuffer() {
        if (this.buffer != null)
            this.buffer.clear();
    }
}

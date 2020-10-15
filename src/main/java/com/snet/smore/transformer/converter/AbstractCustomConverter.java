package com.snet.smore.transformer.converter;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public abstract class AbstractCustomConverter {
    private Path path;
    private FileChannel sourceFileChannel;
    protected ByteBuffer buffer;

    public AbstractCustomConverter(Path path) throws Exception {
        this.path = path;

        this.sourceFileChannel = FileChannel.open(this.path, StandardOpenOption.READ);
        this.buffer = ByteBuffer.allocateDirect((int) Files.size(path));
        this.sourceFileChannel.read(buffer);
        this.buffer.flip();
        this.sourceFileChannel.close();
    }

    public abstract void convert();

    public Path getPath() {
        return this.path;
    }

    public void clearBuffer() {
        if (this.buffer != null)
            this.buffer.clear();
    }
}

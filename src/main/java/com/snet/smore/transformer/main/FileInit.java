package com.snet.smore.transformer.main;

import com.snet.smore.common.util.EnvManager;
import com.snet.smore.common.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

@Slf4j
public class FileInit {
    public static void main(String[] args) {
        FileUtil.initFiles(Paths.get(EnvManager.getProperty("transformer.source.file.dir")));
    }
}

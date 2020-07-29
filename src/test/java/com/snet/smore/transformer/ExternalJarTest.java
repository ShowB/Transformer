package com.snet.smore.transformer;

import com.jdotsoft.jarloader.JarClassLoader;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class ExternalJarTest {
    @Test
    public void test() throws Exception {
        File root = new File("converter");
        File[] files = root.listFiles();

        if (files.length < 1)
            return;

        for (File f : files) {
            URL url = f.toURI().toURL();

            URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
            Method loaderMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            loaderMethod.setAccessible(true);
            loaderMethod.invoke(classLoader, url);
        }

        try {
            Class clazz = Class.forName("com.snet.smore.transformer.converter.Sample");
            Method convertMethod = clazz.getDeclaredMethod("convert");
            convertMethod.invoke(clazz.newInstance());

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}

package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ProcessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProcessFileImpl implements ProcessFile {

    private Logger LOG = LoggerFactory.getLogger(ProcessFileImpl.class);

    @Override
    public void asyncProcessFile(String fileId, Path workingDir, File file) {
        //sudo apt install poppler-utils
        //https://stackoverflow.com/questions/77410607/zorin-os-python-installation-error-with-pyenv-for-no-module-named-ssl
        //pdftoppm doc1.pdf doc -png

        String workPath = "\"" + workingDir.toString() + "\"";
        String filePath = "\"" + file.getAbsolutePath() + "\"";
        String name = "\"" + file.getName() + "\"";

        String pyQwen = "/home/debian/qwen-72b.py";
        String pyBin = "/home/debian/.pyenv/shims/python";

        try {
            String[] cmd = {"/bin/sh", "-c", "cd " + workPath + " && " + "/usr/bin/pdftoppm " + filePath + " " + name + " -png"};
            LOG.info("Executing command: {}", String.join(" ", cmd));
            Process res = Runtime.getRuntime().exec(cmd);
            res.waitFor();
            LOG.info("Done converting pdf to images");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        LOG.info("Ls {}", workingDir);
        String argImages = Arrays.stream(workingDir.toFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".png");
            }
        }))
        .map(File::getAbsolutePath)
        .sorted()
        .collect(Collectors.joining(","));

        LOG.info("Ls res {}", argImages);

        argImages = "\"" + argImages + "\"";

        String fileName = "\"" + file.getName().split("\\.")[1] + "\"";

        try {
            String[] cmd = {"/bin/bash", pyBin, pyQwen, fileId, fileName, argImages};
            LOG.info("Executing command: {}", String.join(" ", cmd));
            Process res = Runtime.getRuntime().exec(cmd);
            res.waitFor();
            LOG.info("Done python Qwen request");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

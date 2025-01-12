package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ProcessFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class ProcessFileImpl implements ProcessFile {

    private Logger LOG = LoggerFactory.getLogger(ProcessFileImpl.class);

    @Override
    public void asyncProcessFile(File file) {
        //sudo apt install poppler-utils
        //https://stackoverflow.com/questions/77410607/zorin-os-python-installation-error-with-pyenv-for-no-module-named-ssl
        //pdftoppm doc1.pdf doc -png

        String path = "\"" + file.getAbsolutePath() + "\"";
        String name = "\"" + file.getName() + "\"";


        try {
            //Process p = Runtime.getRuntime().exec(new String[]{"bash","-c","/usr/bin/pdftoppm", path, name, "-png"});
            String[] cmd = {"bash","-c","/usr/bin/pdftoppm", path, name, "-png"};

            LOG.info("Executing command: {}", String.join(" ", cmd));
            Process res = Runtime.getRuntime().exec(cmd);




        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

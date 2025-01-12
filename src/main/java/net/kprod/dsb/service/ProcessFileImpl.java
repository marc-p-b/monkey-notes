package net.kprod.dsb.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ProcessFileImpl implements ProcessFile {

        @Override
    public void asyncProcessFile(File file) {
            //sudo apt install poppler-utils
            //https://stackoverflow.com/questions/77410607/zorin-os-python-installation-error-with-pyenv-for-no-module-named-ssl
            //pdftoppm doc1.pdf doc -png

            String path = file.getAbsolutePath();
            String name = file.getName();

            String cmd = new StringBuilder().append("bash").append("-c").append("/usr/bin/pdftoppm").append(path).append(name).append("-png").toString();
            System.out.println("run " + cmd);
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"bash","-c","/usr/bin/pdftoppm", path, name, "-png"});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
}

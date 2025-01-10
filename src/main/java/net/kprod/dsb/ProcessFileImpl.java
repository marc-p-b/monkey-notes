package net.kprod.dsb;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ProcessFileImpl implements ProcessFile {

        @Override
    public void asyncProcessFile(File file) {
            //sudo apt install poppler-utils
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

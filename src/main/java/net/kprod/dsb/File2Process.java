package net.kprod.dsb;

import java.io.File;
import java.nio.file.Path;

public class File2Process {
        private String fileId;
        private Path workingDir;
        private File file;
        private String md5;

        public File2Process(String fileId, Path workingDir, File file) {
            this.fileId = fileId;
            this.workingDir = workingDir;
            this.file = file;
        }

        public String getFileId() {
            return fileId;
        }

        public Path getWorkingDir() {
            return workingDir;
        }

        public File getFile() {
            return file;
        }

    public String getMd5() {
        return md5;
    }

    public File2Process setMd5(String md5) {
        this.md5 = md5;
        return this;
    }
}
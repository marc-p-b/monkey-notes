package net.kprod.dsb.data;

public class RemoteFile {
        private String fileId;
        private String fileName;
        private String remoteFolder;
        private String md5;
        private String parentFolderId;
        private String parentFolderName;
        private boolean marked4Update;

        public String getFileId() {
                return fileId;
        }

        public RemoteFile setFileId(String fileId) {
                this.fileId = fileId;
                return this;
        }

        public String getFileName() {
                return fileName;
        }

        public RemoteFile setFileName(String fileName) {
                this.fileName = fileName;
                return this;
        }

        public String getRemoteFolder() {
                return remoteFolder;
        }

        public RemoteFile setRemoteFolder(String remoteFolder) {
                this.remoteFolder = remoteFolder;
                return this;
        }

        public String getMd5() {
                return md5;
        }

        public RemoteFile setMd5(String md5) {
                this.md5 = md5;
                return this;
        }

        public String getParentFolderId() {
                return parentFolderId;
        }

        public RemoteFile setParentFolderId(String parentFolderId) {
                this.parentFolderId = parentFolderId;
                return this;
        }

        public String getParentFolderName() {
                return parentFolderName;
        }

        public RemoteFile setParentFolderName(String parentFolderName) {
                this.parentFolderName = parentFolderName;
                return this;
        }

        public boolean isMarked4Update() {
                return marked4Update;
        }

        public RemoteFile setMarked4Update(boolean marked4Update) {
                this.marked4Update = marked4Update;
                return this;
        }
}
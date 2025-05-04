package net.kprod.dsb.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;

public interface ExportService {
    void export(OutputStream outputStream) throws IOException;
    void importUserData(MultipartFile multipartFile);
}

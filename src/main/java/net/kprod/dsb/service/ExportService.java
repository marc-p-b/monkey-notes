package net.kprod.dsb.service;

import java.io.IOException;
import java.io.OutputStream;

public interface ExportService {
    void export(OutputStream outputStream) throws IOException;
}

package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
public class DataController {
    private Logger LOG = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private ExportService exportService;

    @PostMapping("/data/import")
    public ResponseEntity<String> importFile(@RequestParam("file") MultipartFile multipartFile) {
        exportService.importUserData(multipartFile);
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/data/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> exportUserData() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportService.export(baos);
        byte[] data = baos.toByteArray();
        ByteArrayResource resource = new ByteArrayResource(data);

        //TODO streaming has some issues with vue.js frontend
        //StreamingResponseBody stream = outputStream -> {
        //    exportService.export(baos);
        //};
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=local-files.zip")
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(stream);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

}
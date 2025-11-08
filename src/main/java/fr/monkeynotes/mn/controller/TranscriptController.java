package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptDetails;
import fr.monkeynotes.mn.data.dto.FileNode;
import fr.monkeynotes.mn.data.enums.ViewOptionsCompletionStatus;
import fr.monkeynotes.mn.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
public class TranscriptController {
    @Autowired
    private ViewService viewService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private DriveUtilsService driveUtilsService;

    @Autowired
    private UpdateService updateService;

    @Autowired
    private EditService editService;

    @Autowired
    private DriveChangeManagerService driveChMgmtService;
    @Autowired
    private NamedEntitiesService namedEntitiesService;

    @GetMapping("/transcript/{fileId}")
    public ResponseEntity<DtoTranscript> getTranscript(@PathVariable String fileId) throws IOException {
        return ResponseEntity.ok(viewService.getTranscript(fileId, ViewOptions.all()));
    }

    @GetMapping("/transcript/{fileId}/failed")
    public ResponseEntity<DtoTranscript> getTranscriptFailedPages(@PathVariable String fileId) throws IOException {
        return ResponseEntity.ok(viewService.getTranscript(fileId, new ViewOptions().setCompletionStatus(ViewOptionsCompletionStatus.failed)));
    }

    @GetMapping("/transcript/recent")
    public ResponseEntity<List<DtoTranscriptDetails>> viewRecentTranscripts() throws IOException {
        return ResponseEntity.ok().body(viewService.listRecentTranscripts(0, 10));
    }

    @GetMapping(value = "/transcript/pdf/{fileId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getTranscriptPdf(@PathVariable String fileId) throws IOException {
        File file = viewService.createTranscriptPdf(fileId);

        StreamingResponseBody stream = outputStream -> {
            utilsService.efficientStreamFile(file, outputStream);
        };

        String docName = driveUtilsService.getFileName(fileId) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + docName)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.length())
                .body(stream);
    }

    @GetMapping("/transcript/update/all")
    public ResponseEntity<String> updateAll() {
        updateService.updateAll();
        return ResponseEntity.ok().body("Update root folder requested");
    }

    @GetMapping("/transcript/folder/update/{folderId}")
    public ResponseEntity<String> updateFolder(@PathVariable String folderId) {
        updateService.updateFolder(folderId);
        return ResponseEntity.ok().body("Folder update is requested");
    }

    @GetMapping("/transcript/update/{fileId}/{pageNumber}")
    public ResponseEntity<String> formUpdateTranscriptPage(@PathVariable String fileId, @PathVariable int pageNumber) {
        updateService.forcePageUpdate(fileId, pageNumber);
        return ResponseEntity.ok().body("OK");
    }

    @PostMapping("/transcript/edit/{fileId}/{pageNumber}")
    public ResponseEntity<String> formEditTranscriptPage(@PathVariable String fileId, @PathVariable int pageNumber, @RequestBody String content) {
        editService.edit(fileId, pageNumber, content);
        namedEntitiesService.saveNamedEntitiesFromContent(fileId, pageNumber, content);
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping(value = "/transcript/folder/pdf/{folderId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> getFolderPdf(@PathVariable String folderId) throws IOException {
        File file = viewService.createTranscriptPdfFromFolder(folderId);

        StreamingResponseBody stream = outputStream -> {
            utilsService.efficientStreamFile(file, outputStream);
        };

        String docName = driveUtilsService.getFileName(folderId) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + docName)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.length())
                .body(stream);
    }

    @GetMapping("/transcript/update/{fileId}")
    public ResponseEntity<String> forceUpdateTranscript(@PathVariable String fileId) {
        updateService.requestForceTranscriptUpdate(fileId);
        return ResponseEntity.ok().body("Transcript update requested");
    }

    @GetMapping("/transcript/folder/list")
    public ResponseEntity<List<FileNode>> viewRootFolders() throws IOException {
        return ResponseEntity.ok().body(viewService.listRootLevel());
    }

//    @GetMapping("/folder/list")
//    public ResponseEntity<List<FileNode>> viewFolders() {
//        return ResponseEntity.ok().body(viewService.listAllNodes());

    @GetMapping("/transcript/folder/list/{folderId}")
    public ResponseEntity<List<FileNode>> viewFolder(@PathVariable String folderId) throws IOException {
        List<FileNode> l = viewService.listLevel(folderId);
        return ResponseEntity.ok().body(l);
    }

    @GetMapping("/transcript/delete/{fileId}")
    public ResponseEntity<String> delete(@PathVariable String fileId) throws IOException {
        viewService.delete(fileId);
        return ResponseEntity.ok("Object deleted");
    }
}

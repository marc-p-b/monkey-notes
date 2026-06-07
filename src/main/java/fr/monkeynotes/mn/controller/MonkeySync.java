package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.MonkeyFileEvent;
import fr.monkeynotes.mn.data.SyncEventResponse;
import fr.monkeynotes.mn.data.entity.EntityTranscript;
import fr.monkeynotes.mn.data.repository.RepositoryTranscript;
import fr.monkeynotes.mn.service.MonkeySyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
public class MonkeySync {

    private Logger LOG = LoggerFactory.getLogger(MonkeySync.class);

    @Autowired
    private MonkeySyncService monkeySyncService;

    @PostMapping(value = "/sync/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SyncEventResponse> syncPdf(
            @RequestPart("event") MonkeyFileEvent fileEvent,
            @RequestPart("file") MultipartFile file) {
        SyncEventResponse syncEventResponse = null;

        try {
            syncEventResponse = monkeySyncService.monkeySyncUpdate(fileEvent, file.getBytes());
        } catch (IOException e) {
            LOG.error("Error while reading file {}", fileEvent.getFilePath(), e);
            return new ResponseEntity<>(syncEventResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        SyncEventResponse.SyncEventStatus status = syncEventResponse.getStatus();
        syncEventResponse.getStatus();

        switch (status) {
            case refused -> {
                return new ResponseEntity<>(syncEventResponse, HttpStatus.NOT_ACCEPTABLE);
            }
            case accepted -> {
                return new ResponseEntity<>(syncEventResponse, HttpStatus.OK);
            }

        }
        return new ResponseEntity<>(syncEventResponse, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Autowired
    private RepositoryTranscript repositoryTranscript;

    @GetMapping(value = "/sync/status/{msId}")
    public ResponseEntity<String> syncUpdate(@PathVariable String msId) {

        LOG.info("request update for {}", msId);

        Optional<EntityTranscript> optionalEntityTranscript = repositoryTranscript.findAllByIdFile_FileId(msId);

        if(optionalEntityTranscript.isPresent()) {
            return ResponseEntity.ok("processed");
        }

        return ResponseEntity.ok("ko");
    }

    @GetMapping(value = "/sync/delete/{msId}")
    public ResponseEntity<String> syncDelete(@PathVariable String msId) {

        return ResponseEntity.ok("ok");
    }

}

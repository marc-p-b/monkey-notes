package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.MonkeyFileEvent;
import fr.monkeynotes.mn.service.UpdateService;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonkeySync {

    private Logger LOG = LoggerFactory.getLogger(MonkeySync.class);

    @Autowired
    private UpdateService updateService;

    @PostMapping(value = "/sync/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> syncPdf(@RequestBody MonkeyFileEvent fileEvent) {

        LOG.info("Received inbound file event name {} type {} folder {} size {}", fileEvent.getFileName(), fileEvent.getEventType(), fileEvent.getPath(), fileEvent.getFileSize());

        updateService.monkeySyncUpdate(fileEvent);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}

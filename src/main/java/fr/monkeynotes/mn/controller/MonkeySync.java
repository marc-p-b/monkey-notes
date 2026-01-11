package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.InboundFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonkeySync {

    private Logger LOG = LoggerFactory.getLogger(MonkeySync.class);

    @PostMapping(value = "/sync/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> syncPdf(@RequestBody InboundFileEvent inboundFileEvent) {

        LOG.info("Received inbound file event name {} type {} folder {} size {}", inboundFileEvent.getFileName(), inboundFileEvent.getEventType(), inboundFileEvent.getPath(), inboundFileEvent.getFileSize());



        return new ResponseEntity<>(HttpStatus.OK);
    }

}

package net.kprod.dsb.controller;

import net.kprod.dsb.service.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
public class DefaultController {
    private Logger LOG = LoggerFactory.getLogger(DefaultController.class);

    @Autowired
    private DriveService driveService;

    @GetMapping("/list")
    public ResponseEntity<String> list() {

        try {
            driveService.list();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/watch")
    public ResponseEntity<String> watch() {
        try {
            driveService.watch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/stop")
    public ResponseEntity<String> stop() {
        try {
            driveService.watchStop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().body("OK");
    }

    @PostMapping("/transcript")
    public ResponseEntity<String> transcript(@RequestBody String body) {
        LOG.info("Received transcript: {}", body);
        return ResponseEntity.ok().body("OK");
    }

    //{"fileId": "aaa2233", "fileName": "aaaa", "text-content": [[{"file": "/tmp/gradio/fe54c96c7be7e2eea17a636136a1614a2061d6386347e9767e06d65e8e1a162f/a-1.png", "alt_text": ""}, null], ["image to text", "The text in the image is:\n\n\"n'\u00e9ions des espoirs ?\""]]}
    //{'fileId': '1ayhzqdX1sFzTXXCM49BKlgEt4oP23O4G', 'fileName': 'pdf', 'text-content': [[{'file': '/tmp/gradio/69b02b88edf59176e0f600ed5502eac85cd8c8a3cd7680b08beab40325725d55/codir 250108.pdf-1.png', 'alt_text': ''}, None], [{'file': '/tmp/gradio/ed4ea45d7c11267638951d01c60e1d500f2cf869af4c2bea43e5b6498fa03581/codir 250108.pdf-2.png', 'alt_text': ''}, None], ['image to text', '### Image 1\n\n- Formation\n  - un collab doit recevoir une formation tous les 6 mois\n  - voeux (Robin ?)\n  - coop fournisseurs\n    - mail envoyé / 30k à ce jour\n\nPoint MARC\n- Migration\n- SMS / T2C\n- Formation tech\n- Restructuration Ne@S6\n\nCA travaux | 20M 217\n| 59500 lead\n| 33,4% publi\n| 12,9% sign\n\n### Image 2\n\nPrépa Présentation\n- Bilan par équipe\n  - Acquis | Bilans chiffrés\n  - Pro / but\n  - Effectifs\n- Budget\n- Identification Qu\n  - commune\n  - tech / produit\n\npubli auto - analyse des commentaires\nassurance x dégâts-des-eaux\nconseil / banque\n-> revoir prez data']]}
    //{"fileId": "17PtS0N0iNoRS2ZwknhZPydHxWgBkVOk2", "fileName": "pdf", "text-content": [[{"file": "/tmp/gradio/81fb23170c7080a5083b5f87468f4bafcb8fbcb62aa2a9a346ca7f747931dad1/230325 - codir.pdf-1.png", "alt_text": ""}, null], ["image to text", "Sure, here is the text from the image:\n\n```\n[] CR\n- NAO : ou / sans Nanc des\n- code pharma petites images <7 24/09\n- fiabilit\u00e9s des pros\n- UPI -> activit\u00e9 commune\n- Journ\u00e9e du 24/14\n\n-> pr\u00e9pa \u00e0 l'avance\n-> soumission et pr\u00e9 travail voir\n-> pr\u00e9sentation de la zone\nl'eau fonde\n\n-> r\u00e9org des espaces ?\n```"]]}

    @PostMapping("/notify")
    public ResponseEntity<String> notifyChange(
            //@RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState,
            //@RequestHeader(value = "X-Goog-Message-Number", required = false) String messageNumber,
            //@RequestHeader(value = "X-Goog-Resource-URI", required = false) String resourceURI,
            @RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId
            ) {

        LOG.info("Received notify: channel {}", channelId);
        //LOG.info("Received Drive Notification, message number {}, state {}", messageNumber, resourceState);
        //LOG.info("Message Number: " + messageNumber);
        //LOG.info("resourceURI: " + resourceURI);

        driveService.getChanges(channelId);

        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }
}
package net.kprod.dsb.controller;

import net.kprod.dsb.service.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

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



    @PostMapping("/notify")
    public ResponseEntity<String> notifyChange(
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState,
            @RequestHeader(value = "X-Goog-Message-Number", required = false) String messageNumber,
            @RequestHeader(value = "X-Goog-Resource-URI", required = false) String resourceURI) {

        LOG.info("Received Drive Notification, message number {}, state {}", messageNumber, resourceState);
        LOG.info("Message Number: " + messageNumber);
        LOG.info("resourceURI: " + resourceURI);

        driveService.getChanges();

        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }





//    @PostMapping("/notify")
//    public ResponseEntity<String> notifyChange(
//    ){
//        LOG.info("Notify changed");
//
//        return new ResponseEntity<>("Notification received", HttpStatus.OK);};




//    @PostMapping("/notify")
//    public ResponseEntity<String> notifyChange(
//
//        @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
//        @RequestHeader(value = "X-Goog-Resource-ID", required = false) String resourceId,
//        @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState,
//        @RequestHeader(value = "X-Goog-Message-Number", required = false) String messageNumber,
//        @RequestHeader(value = "X-Goog-Resource-URI", required = false) String resourceURI,
//        @RequestBody(required = false) String body) {
//
//            // Log les informations de la notification
//        LOG.info("Received Drive Notification, message number {}, state {}", messageNumber, resourceState);
////        LOG.info("Channel ID: " + channelId);
//        //LOG.info("Resource ID: " + resourceId);
//        //LOG.info("Resource State: " + resourceState);
//        LOG.info("Message Number: " + messageNumber);
//        LOG.info("resourceURI: " + resourceURI);
////        LOG.info("Body: " + body);
////
////            // Vérifiez l'état de la ressource (ex: "add", "update", "delete")
////            if (resourceState != null) {
////                switch (resourceState) {
////                    case "add":
////                        LOG.info("A new resource was added.");
////                        break;
////                    case "update":
////                        LOG.info("A resource was updated.");
////                        break;
////                    case "delete":
////                        LOG.info("A resource was deleted.");
////                        break;
////                    default:
////                        LOG.info("Unknown resource state: " + resourceState);
////                }
////            }
//
//
//
//        driveService.getChanges();
//
//            // Retourner un statut HTTP 200 pour confirmer la réception
//            return new ResponseEntity<>("Notification received", HttpStatus.OK);
//        }




}

package net.kprod.mn.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
public class TestController {

    @GetMapping("/test/recent")
    public ResponseEntity<List<String>> viewRecentTranscripts() throws IOException {
        System.out.println("test auth page");
        return ResponseEntity.ok().body(Arrays.asList("aaa","bbb","ccc"));
    }



}

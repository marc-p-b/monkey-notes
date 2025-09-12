package net.kprod.mn.controller;

import net.kprod.mn.data.dto.DtoTranscriptDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
public class TestController {

    @GetMapping("/test/recent")
    public ResponseEntity<List<String>> viewRecentTranscripts() throws IOException {
        System.out.println("recent!!!");
        return ResponseEntity.ok().body(Arrays.asList("aaa","bbb","ccc"));
    }



}

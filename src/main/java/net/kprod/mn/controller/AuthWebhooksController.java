package net.kprod.mn.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.kprod.mn.JwtUtil;
import net.kprod.mn.service.DriveChangeManagerService;
import net.kprod.mn.service.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.util.ArrayList;

@Controller
public class AuthWebhooksController {
    private Logger LOG = LoggerFactory.getLogger(AuthWebhooksController.class);

    @Autowired
    private DriveService driveService;

    @Autowired
    private DriveChangeManagerService driveChMgmtService;

//    @GetMapping("/grant-callback")
//    public ResponseEntity<String> grantCallback(HttpServletRequest request) throws IOException {
//        String code = request.getParameter("code");
//        String state = request.getParameter("state");
//
//        String token = state.split("=")[1];
//
//        if (JwtUtil.validateToken(token)) {
//            String username = JwtUtil.extractUsername(token);
//            var auth = new UsernamePasswordAuthenticationToken(username, token, new ArrayList<>());
//            SecurityContextHolder.getContext().setAuthentication(auth);
//        }
//
//        driveService.grantCallback(code);
//        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
//                .header("Location", "/").build();
//        //return "redirect:localhost:5173";
//    }

    @GetMapping("/grant-callback")
    public String grantCallback(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        String state = request.getParameter("state");

        String token = state.split("=")[1];

        if (JwtUtil.validateToken(token)) {
            String username = JwtUtil.extractUsername(token);
            var auth = new UsernamePasswordAuthenticationToken(username, token, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        driveService.grantCallback(code);

        return "redirect:http://localhost:5173/preferences";
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notifyChange(@RequestHeader(value = "X-Goog-Channel-Id", required = false) String channelId) {
        driveChMgmtService.changeNotified(channelId);
        return new ResponseEntity<>("Notification received", HttpStatus.OK);
    }
}
package net.kprod.mn.controller;

import net.kprod.mn.data.dto.DtoConfigs;
import net.kprod.mn.data.dto.DtoGoogleDriveConnect;
import net.kprod.mn.service.DriveService;
import net.kprod.mn.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Optional;

@Controller
public class PreferencesController {

    @Autowired
    private PreferencesService preferencesService;

    @Autowired
    private DriveService driveService;

    @GetMapping("/preferences/get")
    public ResponseEntity<DtoConfigs> preferences(Model model) {
        return ResponseEntity.ok(preferencesService.listPreferences());
    }

    @GetMapping("/preferences/authGoogleDrive")
    public ResponseEntity<DtoGoogleDriveConnect> auth() {
        Optional<String> optAuthUrl = driveService.requireAuth();
        if (optAuthUrl.isPresent()) {
            return ResponseEntity.ok(DtoGoogleDriveConnect.disconnected(optAuthUrl.get()));
        } else {
            return ResponseEntity.ok(new DtoGoogleDriveConnect());
        }
    }

    @GetMapping("/preferences/reset")
    public String reset(Model model) {
        preferencesService.resetPreference();

        model.addAttribute("preferences", preferencesService.listPreferences());

        return "preferences";
    }

    @PostMapping("/preferences/form")
    public String savePreferences(Model model, @RequestParam Map<String, String> formData) {
        preferencesService.setPreference(formData);
        model.addAttribute("preferences", preferencesService.listPreferences());
        return "preferences";
    }



}

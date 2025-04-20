package net.kprod.dsb.controller;

import net.kprod.dsb.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.prefs.Preferences;

@Controller
public class PreferencesController {

    @Autowired
    private PreferencesService preferencesService;

    @GetMapping("/preferences")
    public String preferences(Model model) {
        model.addAttribute("preferences", preferencesService.listPreferences());

        return "preferences";
    }

    @GetMapping("/preferences/reset")
    public String reset(Model model) {
        preferencesService.resetPreference();

        model.addAttribute("preferences", preferencesService.listPreferences());

        return "preferences";
    }

    @PostMapping("/form/preferences")
    public String savePreferences(@RequestParam Map<String, String> formData) {
        System.out.println(formData);
        return "preferences";
    }



}

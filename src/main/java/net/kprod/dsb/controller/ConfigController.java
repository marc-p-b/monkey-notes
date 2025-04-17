package net.kprod.dsb.controller;

import net.kprod.dsb.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConfigController {

    @Autowired
    private PreferencesService preferencesService;

    @GetMapping("/preferences")
    public String preferences(Model model) {

        preferencesService.listPreferences();

        return "preferences";
    }



}

package net.kprod.dsb.controller;

import net.kprod.dsb.data.entity.EntityConfig;
import net.kprod.dsb.data.entity.EntityConfigId;
import net.kprod.dsb.data.enums.ConfigKey;
import net.kprod.dsb.data.repository.RepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

@Controller
public class ConfigController {


    @Autowired
    private RepositoryConfig repositoryConfig;


    @Value("${app.qwen.model}")
    private String qwenModel;

    @Value("${app.qwen.prompt}")
    private String qwenPrompt;

    @GetMapping("/preferences")
    public String preferences(Model model) {

        String username = "marc";

        EntityConfigId entityConfigId = new EntityConfigId(username, ConfigKey.set);

        Optional<EntityConfig> optConfig = repositoryConfig.findByConfigId(entityConfigId);

        if(optConfig.isEmpty()) {
            List<EntityConfig> list = null;


                list = Arrays.asList(
                    new EntityConfig(new EntityConfigId(username, ConfigKey.set), "done"),
                    new EntityConfig(new EntityConfigId(username, ConfigKey.prompt), qwenPrompt),
                    new EntityConfig(new EntityConfigId(username, ConfigKey.model), qwenModel),
                    new EntityConfig(new EntityConfigId(username, ConfigKey.useDefaultPrompt), "true"));


            repositoryConfig.saveAll(list);


        }

        return "preferences";
    }



}

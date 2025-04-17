package net.kprod.dsb.service.impl;

import net.kprod.dsb.data.dto.DtoConfig;
import net.kprod.dsb.data.entity.EntityConfig;
import net.kprod.dsb.data.entity.EntityConfigId;
import net.kprod.dsb.data.enums.ConfigKey;
import net.kprod.dsb.data.repository.RepositoryConfig;
import net.kprod.dsb.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PreferencesServiceImpl implements PreferencesService {
    @Autowired
    private RepositoryConfig repositoryConfig;

    @Value("${app.qwen.model}")
    private String qwenModel;

    @Value("${app.qwen.prompt}")
    private String qwenPrompt;

    @Override
    public List<DtoConfig> listPreferences() {

        String username = "marc";

        EntityConfigId entityConfigId = new EntityConfigId(username, ConfigKey.set);

        Optional<EntityConfig> optConfig = repositoryConfig.findByConfigId(entityConfigId);

        if(optConfig.isEmpty()) {
            return initPreferences(username);
        } else {
            return Arrays.stream(ConfigKey.values())
                    .map(k -> {
                        return getConfig(username, k);
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
    }

    private Optional<DtoConfig> getConfig(String username, ConfigKey key) {
        Optional<EntityConfig> config = repositoryConfig.findByConfigId(new EntityConfigId(username, key));
        return config.isPresent() ? Optional.of(DtoConfig.fromEntity(config.get())) : Optional.empty();
    }


    private List<DtoConfig> initPreferences(String username) {
        List<EntityConfig> list = Arrays.asList(
                new EntityConfig(new EntityConfigId(username, ConfigKey.set), "done"),
                new EntityConfig(new EntityConfigId(username, ConfigKey.prompt), qwenPrompt),
                new EntityConfig(new EntityConfigId(username, ConfigKey.model), qwenModel),
                new EntityConfig(new EntityConfigId(username, ConfigKey.useDefaultPrompt), "true"));

        repositoryConfig.saveAll(list);

        return list.stream()
                .map(DtoConfig::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public DtoConfig getPreference(ConfigKey configKey) {
        return null;
    }

    @Override
    public void setPreference(DtoConfig dtoConfig) {

    }
}

package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoConfig;
import net.kprod.dsb.data.entity.EntityConfig;
import net.kprod.dsb.data.entity.EntityConfigId;
import net.kprod.dsb.data.enums.ConfigKey;
import net.kprod.dsb.data.repository.RepositoryConfig;
import net.kprod.dsb.service.AuthService;
import net.kprod.dsb.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PreferencesServiceImpl implements PreferencesService {

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryConfig repositoryConfig;

    @Value("${app.qwen.model}")
    private String qwenModel;

    @Value("${app.qwen.prompt}")
    private String qwenPrompt;

    @Override
    public List<DtoConfig> listPreferences() {
        if(isParametersSet() == false) {
            return initPreferences(authService.getConnectedUsername());
        } else {
            List<DtoConfig> list = Arrays.stream(ConfigKey.values())
                    .map(k -> {
                        return getConfig(authService.getConnectedUsername(), k);
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            return list;
        }
    }

    private boolean isParametersSet() {
        EntityConfigId entityConfigId = new EntityConfigId(authService.getConnectedUsername(), ConfigKey.set);
        Optional<EntityConfig> optEntity = repositoryConfig.findByConfigId(entityConfigId);
        return optEntity.isPresent();
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
                new EntityConfig(new EntityConfigId(username, ConfigKey.useDefaultPrompt), "true"),
                new EntityConfig(new EntityConfigId(username, ConfigKey.inputFolderId), ""),
                new EntityConfig(new EntityConfigId(username, ConfigKey.outputFolderId), "")
        );

        repositoryConfig.saveAll(list);

        return list.stream()
                .map(DtoConfig::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Object getPreference(ConfigKey configKey) throws ServiceException {
        if(isParametersSet() == false) {
            throw new ServiceException("Preferences not set");
        }

        Optional<EntityConfig> optValue = repositoryConfig.findByConfigId(new EntityConfigId(authService.getConnectedUsername(), configKey));
        if(optValue.isEmpty()) {
            throw new ServiceException(configKey + " not set");
        }
        String value = optValue.get().getValue();

        Object o = switch (configKey) {
            case useDefaultPrompt -> Boolean.parseBoolean(value);
            case modelTimeout -> Integer.parseInt(value);
            default -> value;
        };
        return o;
    }

    public String getInputFolderId() throws ServiceException {
        return this.getPreference(ConfigKey.inputFolderId).toString();
    }

    public String getOutputFolderId() throws ServiceException {
        return this.getPreference(ConfigKey.outputFolderId).toString();
    }

    @Override
    public void setPreference(Map<String, String> formData) {
        List<DtoConfig> list = new ArrayList<>();
        formData.forEach((k, v) -> {
            list.add(new DtoConfig(authService.getConnectedUsername(), ConfigKey.valueOf(k), v));
        });
        list.add(new DtoConfig(authService.getConnectedUsername(), ConfigKey.set, "done"));
        List<EntityConfig> listE = list.stream()
                .map(DtoConfig::toEntity)
                .toList();
        repositoryConfig.saveAll(listE);
    }

    @Override
    @Transactional
    public void resetPreference() {
        repositoryConfig.deleteByConfigId_Username(authService.getConnectedUsername());
    }
}

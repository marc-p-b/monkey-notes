package net.kprod.dsb.service.impl;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoConfigs;
import net.kprod.dsb.data.entity.EntityPreferences;
import net.kprod.dsb.data.entity.EntityPreferencesId;
import net.kprod.dsb.data.enums.PreferenceKey;
import net.kprod.dsb.data.repository.RepositoryConfig;
import net.kprod.dsb.service.AuthService;
import net.kprod.dsb.service.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PreferencesServiceImpl implements PreferencesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesServiceImpl.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryConfig repositoryConfig;

    @Value("${app.defaults.qwen.model}")
    private String dftQwenModel;

    @Value("${app.defaults.qwen.prompt}")
    private String dftQwenPrompt;

    @Value("${app.defaults.qwen.max-tokens}")
    private int dftQwenMaxTokens;

    @Value("${app.defaults.ai.connect-timeout}")
    private int dftAiConnectTimeout;

    @Value("${app.defaults.ai.read-timeout}")
    private int dftAiReadTimeout;

    @Override
    public DtoConfigs listPreferences() {

        if(isParametersSet() == false) {
            return initPreferences(authService.getConnectedUsername());
        } else {
            Map<String, String> map = repositoryConfig.findAllByConfigId_Username(authService.getConnectedUsername()).stream()
                    .collect(Collectors.toMap(entityConfig -> entityConfig.getConfigId().getKey(), entityConfig -> entityConfig.getValue()));
            return fromMap(map);

        }
    }

    private DtoConfigs fromMap(Map<String, String> map) {
        DtoConfigs dtoConfigs = new DtoConfigs();
        for (Map.Entry<String, String> entry : map.entrySet()) {

            try {

                PreferenceKey key = PreferenceKey.valueOf(entry.getKey());
                switch (key) {
                    case prompt :
                        dtoConfigs.setPrompt(entry.getValue());
                        break;
                    case model:
                        dtoConfigs.setModel(entry.getValue());
                        break;
                    case inputFolderId:
                        dtoConfigs.setInputFolderId(entry.getValue());
                        break;
                    case outputFolderId:
                        dtoConfigs.setOutputFolderId(entry.getValue());
                        break;
                    case set:
                        dtoConfigs.setSet(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case useDefaultPrompt:
                        dtoConfigs.setUseDefaultPrompt(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case useDefaultModel:
                        dtoConfigs.setUseDefaultModel(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case useDefaultAiConnectTimeout:
                        dtoConfigs.setUseDefaultAiConnectTimeout(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case useDefaultAiReadTimeout:
                        dtoConfigs.setUseDefaultAiReadTimeout(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case useDefaultModelMaxTokens:
                        dtoConfigs.setUseDefaultModelMaxTokens(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case aiConnectTimeout:
                        dtoConfigs.setAiConnectTimeout(Integer.parseInt(entry.getValue()));
                        break;
                    case aiReadTimeout:
                        dtoConfigs.setAiReadTimeout(Integer.parseInt(entry.getValue()));
                        break;
                    case modelMaxTokens:
                        dtoConfigs.setModelMaxTokens(Integer.parseInt(entry.getValue()));
                        break;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Unknown config key {}", entry.getKey());
            }

        }
        return dtoConfigs;
    }

    public boolean isParametersSet() {
        EntityPreferencesId entityConfigId = new EntityPreferencesId(authService.getConnectedUsername(), PreferenceKey.set);
        Optional<EntityPreferences> optEntity = repositoryConfig.findByConfigId(entityConfigId);
        return optEntity.isPresent();
    }

    public boolean isParametersNotSet() {
        return !isParametersSet();
    }

    private DtoConfigs initPreferences(String username) {

        DtoConfigs dtoConfigs = new DtoConfigs()
                .setUseDefaultPrompt(true)
                .setPrompt(dftQwenPrompt)
                .setUseDefaultPrompt(true)
                .setModel(dftQwenModel)
                .setInputFolderId("")
                .setOutputFolderId("")
                .setUseDefaultAiConnectTimeout(true)
                .setAiConnectTimeout(dftAiConnectTimeout)
                .setUseDefaultAiReadTimeout(true)
                .setAiReadTimeout(dftAiReadTimeout)
                .setUseDefaultModelMaxTokens(true)
                .setModelMaxTokens(dftQwenMaxTokens)
                .setSet(false);

        List<EntityPreferences> list = dtoConfigs.toEntities(username);
        repositoryConfig.saveAll(list);

        return dtoConfigs;
    }

    private String getPreference(PreferenceKey configKey) throws ServiceException {
        if(isParametersSet() == false) {
            throw new ServiceException("Preferences not set");
        }

        Optional<EntityPreferences> optValue = repositoryConfig.findByConfigId(new EntityPreferencesId(authService.getConnectedUsername(), configKey));
        if(optValue.isEmpty()) {
            throw new ServiceException(configKey + " not set");
        }
        return optValue.get().getValue();
    }

    @Override
    public void setPreference(Map<String, String> formData) {
        DtoConfigs dtoConfigs = fromMap(formData)
            .setSet(true);
        repositoryConfig.saveAll(dtoConfigs.toEntities(authService.getConnectedUsername()));
    }

    @Override
    @Transactional
    public void resetPreference() {
        repositoryConfig.deleteByConfigId_Username(authService.getConnectedUsername());
    }


    @Override
    public String getPrompt() throws ServiceException {
        return getPreference(PreferenceKey.prompt);
    }

    @Override
    public String getModel() throws ServiceException {
        return getPreference(PreferenceKey.model);
    }

    @Override
    public boolean useDefaultPrompt() throws ServiceException {
        return Boolean.valueOf(getPreference(PreferenceKey.useDefaultPrompt));
    }

    @Override
    public boolean useDefaultModel() throws ServiceException {
        return Boolean.valueOf(getPreference(PreferenceKey.useDefaultModel));
    }

    @Override
    public String getInputFolderId() throws ServiceException {
        return getPreference(PreferenceKey.inputFolderId);
    }

    @Override
    public String getOutputFolderId() throws ServiceException {
        return getPreference(PreferenceKey.outputFolderId);
    }

    @Override
    public boolean useDefaultAiConnectTimeout() throws ServiceException {
        return Boolean.valueOf(getPreference(PreferenceKey.useDefaultAiReadTimeout));
    }

    @Override
    public int getAiConnectTimeout() throws ServiceException {
        return Integer.valueOf(getPreference(PreferenceKey.aiConnectTimeout));
    }

    @Override
    public boolean useDefaultAiReadTimeout() throws ServiceException {
        return Boolean.valueOf(getPreference(PreferenceKey.useDefaultAiReadTimeout));
    }

    @Override
    public int getAiReadTimeout() throws ServiceException {
        return Integer.valueOf(getPreference(PreferenceKey.aiReadTimeout));
    }

    @Override
    public boolean useDefaultModelMaxTokens() throws ServiceException {
        return Boolean.valueOf(getPreference(PreferenceKey.useDefaultModelMaxTokens));
    }

    @Override
    public int getModelMaxTokens() throws ServiceException {
        return Integer.valueOf(getPreference(PreferenceKey.modelMaxTokens));
    }
}

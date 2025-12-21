package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.dto.DtoPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferencesId;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.repository.RepositoryConfig;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.PreferencesService;
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

    @Value("${app.openai.assistant.defaults.instructions}")
    private String dftOpenaiAssistantInstructions;

    @Override
    public DtoPreferences listPreferences() {

        if(isParametersSet() == false) {
            return initPreferences(authService.getUsernameFromContext());
        } else {
            Map<String, String> map = repositoryConfig.findAllByConfigId_Username(authService.getUsernameFromContext()).stream()
                    .collect(Collectors.toMap(entityConfig -> entityConfig.getConfigId().getKey(), entityConfig -> entityConfig.getValue()));
            return fromMap(map)
                    .setUsername(authService.getUsernameFromContext());
        }
    }

    private DtoPreferences fromMap(Map<String, String> map) {
        //TODO simplify ? remove map may be possible
        DtoPreferences dtoConfigs = new DtoPreferences();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                PreferenceKey key = PreferenceKey.valueOf(entry.getKey());
                switch (key) {
                    case set:
                        dtoConfigs.setInitialized(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case agentInstructions:
                        dtoConfigs.setAgentInstructions(entry.getValue());
                        break;
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
        EntityPreferencesId entityConfigId = new EntityPreferencesId(authService.getUsernameFromContext(), PreferenceKey.set);
        Optional<EntityPreferences> optEntity = repositoryConfig.findByConfigId(entityConfigId);
        return optEntity.isPresent();
    }

    public boolean isParametersNotSet() {
        return !isParametersSet();
    }

    private DtoPreferences initPreferences(String username) {

        DtoPreferences dtoConfigs = new DtoPreferences()
                .setUseDefaultPrompt(true)
                .setPrompt(dftQwenPrompt)
                .setUseDefaultPrompt(true)
                .setModel(dftQwenModel)
                .setAgentInstructions(dftOpenaiAssistantInstructions)
                .setInputFolderId("")
                .setOutputFolderId("")
                .setUseDefaultAiConnectTimeout(true)
                .setAiConnectTimeout(dftAiConnectTimeout)
                .setUseDefaultAiReadTimeout(true)
                .setAiReadTimeout(dftAiReadTimeout)
                .setUseDefaultModelMaxTokens(true)
                .setModelMaxTokens(dftQwenMaxTokens)
                .setInitialized(false);

        List<EntityPreferences> list = dtoConfigs.toEntities(username);
        repositoryConfig.saveAll(list);

        return dtoConfigs;
    }

    private String getPreference(PreferenceKey configKey) throws ServiceException {
        if(isParametersSet() == false) {
            throw new ServiceException("Preferences not set");
        }

        Optional<EntityPreferences> optValue = repositoryConfig.findByConfigId(new EntityPreferencesId(authService.getUsernameFromContext(), configKey));
        if(optValue.isEmpty()) {
            throw new ServiceException(configKey + " not set");
        }
        return optValue.get().getValue();
    }

    @Override
    public void setPreference(DtoPreferences prefs) {
        prefs.setInitialized(true);
        repositoryConfig.saveAll(prefs.toEntities(authService.getUsernameFromContext()));
    }

    @Override
    @Transactional
    public void resetPreference() {
        repositoryConfig.deleteByConfigId_Username(authService.getUsernameFromContext());
    }


    @Override
    public String getPrompt() throws ServiceException {
        return getPreference(PreferenceKey.prompt);
    }

    @Override
    public String getAgentInstructions() throws ServiceException {
        return getPreference(PreferenceKey.agentInstructions);
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

package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.dto.DtoPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferencesId;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.data.enums.SyncOption;
import fr.monkeynotes.mn.data.event.UserCreatedEvent;
import fr.monkeynotes.mn.data.repository.RepositoryConfig;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.PreferencesService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PreferencesServiceImpl implements PreferencesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesServiceImpl.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryConfig repositoryConfig;

    @Value("${app.defaults.sync-option}")
    private SyncOption dftSyncOption;

    @Value("${app.defaults.qwen.prompt}")
    private String dftOCRPrompt;

    @Value("${app.defaults.qwen.max-tokens}")
    private int dftQwenMaxTokens;

    @Value("${app.defaults.ai.connect-timeout}")
    private int dftQwenConnectTimeout;

    @Value("${app.defaults.ai.read-timeout}")
    private int dftQwenReadTimeout;

    @Value("${app.openai.models.instructions}")
    private String dftAgentInstructions;

    @Value("${app.openai.models.available}")
    private String listAgentModelAvailable;

    @Value("${app.openai.models.default}")
    private String dftAgentModel;

    @Value("${app.defaults.qwen.models.available}")
    private String listOcrModelAvailable;

    @Value("${app.defaults.qwen.models.default}")
    private String dftOCRModel;

    @Override
    public DtoPreferences listPreferences() {

        Set<DtoPreferences.AIModel> ocrModels = aiModelsFromConfig(listOcrModelAvailable);
        Set<DtoPreferences.AIModel> agentModels = aiModelsFromConfig(listAgentModelAvailable);

        DtoPreferences dtoPreferences = null;

        if(isParametersSet() == false) {
            dtoPreferences = initPreferences(authService.getUsernameFromContext());
        } else {
            Map<String, String> map = repositoryConfig.findAllByConfigId_Username(authService.getUsernameFromContext()).stream()
                    .collect(Collectors.toMap(entityConfig -> entityConfig.getConfigId().getKey(), entityConfig -> entityConfig.getValue()));
            dtoPreferences = fromMap(map)
                    .setUsername(authService.getUsernameFromContext())
                    .setOcrModels(ocrModels);
        }

        dtoPreferences
                .setAgentModels(agentModels)
                .setOcrModels(ocrModels)
                .setDefaultOcrPrompt(dftOCRPrompt)
                .setDftQwenMaxTokens(dftQwenMaxTokens)
                .setDftQwenConnectTimeout(dftQwenConnectTimeout)
                .setDftQwenReadTimeout(dftQwenReadTimeout)
                .setDftAgentInstructions(dftAgentInstructions);

        return dtoPreferences;
    }

    @Override
    public Set<DtoPreferences.AIModel> aiModelsFromConfig(String listConfig) {
        return Arrays.stream(listConfig.split(","))
                .map(String::trim)
                .map(m -> {
                    String[] mv = m.split("=");
                    return new DtoPreferences.AIModel(mv[0], mv[1]);
                })
                .collect(Collectors.toSet());
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
                    case syncOption:
                        dtoConfigs.setSyncOption(SyncOption.valueOf(entry.getValue()));
                        break;
                    case inputFolderId:
                        dtoConfigs.setInputFolderId(entry.getValue());
                        break;
                    case cropImage:
                        dtoConfigs.setCropImage(Boolean.parseBoolean(entry.getValue()));
                        break;
                    case ocrPrompt:
                        dtoConfigs.setOcrPrompt(entry.getValue());
                        break;
                    case qwenConnectTimeout:
                        dtoConfigs.setQwenConnectTimeout(Integer.parseInt(entry.getValue()));
                        break;
                    case qwenReadTimeout:
                        dtoConfigs.setQwenReadTimeout(Integer.parseInt(entry.getValue()));
                        break;
                    case qwenMaxTokens:
                        dtoConfigs.setOcrMaxTokens(Integer.parseInt(entry.getValue()));
                        break;
                    case selectedOcrModel:
                        dtoConfigs.setSelectedOcrModel(entry.getValue());
                        break;
                    case agentInstructions:
                        dtoConfigs.setAgentInstructions(entry.getValue());
                        break;
                    case selectedAgentModel:
                        dtoConfigs.setSelectedAgentModel(entry.getValue());
                        break;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Unknown config key {}", entry.getKey());
            }
        }
        return dtoConfigs;
    }

    public boolean isParametersSet() {
        return isParametersSet(authService.getUsernameFromContext());
    }

    @Override
    public boolean isParametersSet(String username) {
        EntityPreferencesId entityConfigId = new EntityPreferencesId(username, PreferenceKey.set);
        Optional<EntityPreferences> optEntity = repositoryConfig.findByConfigId(entityConfigId);
        return optEntity.isPresent();
    }

    /**
     * Seeds the default preferences for a freshly created account.
     *
     * Guarded rather than unconditional: initPreferences() ends in a saveAll() that
     * overwrites by primary key, so an unguarded re-run would silently reset a user
     * who had already configured themselves.
     *
     * Deliberately takes the username as a parameter and never touches
     * authService.getUsernameFromContext() - the caller is either an admin creating
     * someone else's account, or the startup bootstrap where no security context exists.
     */
    @Override
    public void initPreferencesIfMissing(String username) {
        if (isParametersSet(username)) {
            return;
        }
        LOGGER.info("Seeding default preferences for user {}", username);
        initPreferences(username);
    }

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        initPreferencesIfMissing(event.username());
    }

    public boolean isParametersNotSet() {
        return !isParametersSet();
    }

    private DtoPreferences initPreferences(String username) {
        Set<DtoPreferences.AIModel> ocrModels = aiModelsFromConfig(listOcrModelAvailable);
        Set<DtoPreferences.AIModel> agentModels = aiModelsFromConfig(listAgentModelAvailable);

        DtoPreferences dtoprefs = new DtoPreferences()
                .setInputFolderId("")
                .setSyncOption(dftSyncOption)
                .setQwenConnectTimeout(dftQwenConnectTimeout)
                .setQwenReadTimeout(dftQwenReadTimeout)
                .setOcrMaxTokens(dftQwenMaxTokens)

                .setSelectedOcrModel(dftOCRModel)
                .setOcrPrompt(dftOCRPrompt)
                .setOcrModels(ocrModels)

                .setAgentModels(agentModels)
                .setAgentInstructions(dftAgentInstructions)
                .setSelectedAgentModel(dftAgentModel)
                .setCropImage(true)
                .setInitialized(false);

        List<EntityPreferences> list = dtoprefs.toEntities(username);
        repositoryConfig.saveAll(list);

        return dtoprefs;
    }

    @Override
    public String getPreference(PreferenceKey configKey) throws ServiceException {
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
    public Optional<String> getPreferenceOpt(PreferenceKey configKey) {
        try{
            return Optional.of(getPreference(configKey));
        } catch (ServiceException e) {
            return Optional.empty();
        }
    }

    public boolean getPreferenceAsBoolean(PreferenceKey configKey) throws ServiceException {
        return Boolean.parseBoolean(getPreference(configKey));
    }

    @Override
    public int getPreferenceAsInt(PreferenceKey configKey) throws ServiceException {
        return Integer.parseInt(getPreference(configKey));
    }

    @Override
    public void setPreference(DtoPreferences prefs) {
        prefs.setInitialized(true);
        repositoryConfig.saveAll(prefs.toEntities(authService.getUsernameFromContext()));
    }

    @Override
    @Transactional
    public void resetPreference() {
        repositoryConfig.deleteAllByConfigId_Username(authService.getUsernameFromContext());
    }


    @Override
    public void setInputFolderId(String id) {
        //TODO only used when using monkeysync
        EntityPreferences entityPreferences = new EntityPreferences(new EntityPreferencesId(authService.getUsernameFromContext(), PreferenceKey.inputFolderId), id);
        repositoryConfig.save(entityPreferences);
    }

    public void setRemoteRootFolderPath(String remoteRootFolderPath) {
        //TODO only used when using monkeysync
        EntityPreferences entityPreferences = new EntityPreferences(new EntityPreferencesId(authService.getUsernameFromContext(), PreferenceKey.remoteRootFolderPath), remoteRootFolderPath);
        repositoryConfig.save(entityPreferences);
    }
}

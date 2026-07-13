package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.dto.DtoPreferences;
import fr.monkeynotes.mn.data.enums.PreferenceKey;

import java.util.Optional;
import java.util.Set;

public interface PreferencesService {
    DtoPreferences listPreferences();
    Set<DtoPreferences.AIModel> aiModelsFromConfig(String listConfig);
    void setPreference(DtoPreferences prefs);
    void resetPreference();
    boolean isParametersSet();
    boolean isParametersNotSet();
    String getPreference(PreferenceKey configKey) throws ServiceException;
    Optional<String> getPreferenceOpt(PreferenceKey configKey);
    boolean getPreferenceAsBoolean(PreferenceKey configKey) throws ServiceException;
    int getPreferenceAsInt(PreferenceKey configKey) throws ServiceException;
    void setInputFolderId(String id);
    void setRemoteRootFolderPath(String remoteRootFolderPath);
}

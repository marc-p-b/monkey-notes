package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.ServiceException;
import fr.monkeynotes.mn.data.dto.DtoPreferences;
import fr.monkeynotes.mn.data.enums.PreferenceKey;

public interface PreferencesService {
    DtoPreferences listPreferences();
    void setPreference(DtoPreferences prefs);
    void resetPreference();
    boolean isParametersSet();
    boolean isParametersNotSet();
    String getPreference(PreferenceKey configKey) throws ServiceException;
    boolean getPreferenceAsBoolean(PreferenceKey configKey) throws ServiceException;
    int getPreferenceAsInt(PreferenceKey configKey) throws ServiceException;
    void setInputFolderId(String id);
}

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

//    boolean useDefaultPrompt() throws ServiceException;
    //String getPrompt() throws ServiceException;

    String getPreference(PreferenceKey configKey) throws ServiceException;
    boolean getPreferenceAsBoolean(PreferenceKey configKey) throws ServiceException;
    int getPreferenceAsInt(PreferenceKey configKey) throws ServiceException;

//    boolean useDefaultModel() throws ServiceException;

//    String getAgentInstructions() throws ServiceException;

//    String getInputFolderId() throws ServiceException;
//    String getOutputFolderId() throws ServiceException;

    //boolean useDefaultAiConnectTimeout() throws ServiceException;
    //int getAiConnectTimeout() throws ServiceException;

    //boolean useDefaultAiReadTimeout() throws ServiceException;
    //int getAiReadTimeout() throws ServiceException;

    //boolean useDefaultModelMaxTokens() throws ServiceException;
    //int getModelMaxTokens() throws ServiceException;
}

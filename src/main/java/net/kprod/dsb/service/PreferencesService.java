package net.kprod.dsb.service;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoConfigs;

import java.util.Map;

public interface PreferencesService {
    DtoConfigs listPreferences();

    void setPreference(Map<String, String> formData);
    void resetPreference();

    boolean isParametersSet();
    boolean isParametersNotSet();

    boolean useDefaultPrompt() throws ServiceException;
    String getPrompt() throws ServiceException;

    boolean useDefaultModel() throws ServiceException;
    String getModel() throws ServiceException;

    String getInputFolderId() throws ServiceException;
    String getOutputFolderId() throws ServiceException;

    boolean useDefaultAiConnectTimeout() throws ServiceException;
    int getAiConnectTimeout() throws ServiceException;

    boolean useDefaultAiReadTimeout() throws ServiceException;
    int getAiReadTimeout() throws ServiceException;

    boolean useDefaultModelMaxTokens() throws ServiceException;
    int getModelMaxTokens() throws ServiceException;
}

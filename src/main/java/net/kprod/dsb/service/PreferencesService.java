package net.kprod.dsb.service;

import net.kprod.dsb.ServiceException;
import net.kprod.dsb.data.dto.DtoConfig;
import net.kprod.dsb.data.enums.ConfigKey;

import java.util.List;
import java.util.Map;

public interface PreferencesService {
    List<DtoConfig> listPreferences();
    Object getPreference(ConfigKey configKey) throws ServiceException;
    String getInputFolderId() throws ServiceException;
    String getOutputFolderId() throws ServiceException;
    void setPreference(Map<String, String> formData);
    void resetPreference();
}

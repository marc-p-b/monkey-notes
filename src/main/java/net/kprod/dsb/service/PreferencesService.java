package net.kprod.dsb.service;

import net.kprod.dsb.data.dto.DtoConfig;
import net.kprod.dsb.data.enums.ConfigKey;

import java.util.List;
import java.util.Map;

public interface PreferencesService {
    List<DtoConfig> listPreferences();
    DtoConfig getPreference(ConfigKey configKey);
    void setPreference(Map<String, String> formData);
    void resetPreference();
}

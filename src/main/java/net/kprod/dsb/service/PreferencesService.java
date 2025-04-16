package net.kprod.dsb.service;

import net.kprod.dsb.data.dto.DtoConfig;
import net.kprod.dsb.data.enums.ConfigKey;

import java.util.List;

public interface PreferencesService {
    List<DtoConfig> listPreferences();
    DtoConfig getPreference(ConfigKey configKey);
    void setPreference(DtoConfig dtoConfig);
}

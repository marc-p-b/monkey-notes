package net.kprod.dsb.service.impl;

import net.kprod.dsb.data.dto.DtoConfig;
import net.kprod.dsb.data.enums.ConfigKey;
import net.kprod.dsb.service.PreferencesService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PreferencesServiceImpl implements PreferencesService {
    @Override
    public List<DtoConfig> listPreferences() {
        return List.of();
    }

    @Override
    public DtoConfig getPreference(ConfigKey configKey) {
        return null;
    }

    @Override
    public void setPreference(DtoConfig dtoConfig) {

    }
}

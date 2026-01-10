package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.EntityPreferences;
import fr.monkeynotes.mn.data.entity.EntityPreferencesId;
import fr.monkeynotes.mn.data.enums.PreferenceKey;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DtoPreferences {
    private boolean initialized = false;
    private String username;

    private String inputFolderId;
    private boolean cropImage;

    private String ocrPrompt;
    private String defaultOcrPrompt;
    private int qwenConnectTimeout;
    private int dftQwenConnectTimeout;
    private int ocrMaxTokens;
    private int dftQwenMaxTokens;
    private int qwenReadTimeout;
    private int dftQwenReadTimeout;
    private Set<String> ocrModels;
    private String selectedOcrModel;

    private String agentInstructions;
    private String dftAgentInstructions;

    public List<EntityPreferences> toEntities(String username) {
        List<EntityPreferences> list = Arrays.asList(
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.set), Boolean.toString(initialized)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.inputFolderId), inputFolderId),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.cropImage), Boolean.toString(cropImage)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.ocrPrompt), ocrPrompt),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.selectedOcrModel), selectedOcrModel),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.qwenConnectTimeout), Integer.toString(qwenConnectTimeout)),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.qwenReadTimeout), Integer.toString(qwenReadTimeout)),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.qwenMaxTokens), Integer.toString(ocrMaxTokens)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.agentInstructions), agentInstructions)
        );
        return list;
    }

    public String getUsername() {
        return username;
    }

    public DtoPreferences setUsername(String user) {
        this.username = user;
        return this;
    }

    public DtoPreferences setInitialized(boolean set) {
        this.initialized = set;
        return this;
    }

    public String getInputFolderId() {
        return inputFolderId;
    }

    public DtoPreferences setInputFolderId(String inputFolderId) {
        this.inputFolderId = inputFolderId;
        return this;
    }

    public boolean isCropImage() {
        return cropImage;
    }

    public DtoPreferences setCropImage(boolean cropImage) {
        this.cropImage = cropImage;
        return this;
    }

    public String getOcrPrompt() {
        return ocrPrompt;
    }

    public DtoPreferences setOcrPrompt(String ocrPrompt) {
        this.ocrPrompt = ocrPrompt;
        return this;
    }

    public String getDefaultOcrPrompt() {
        return defaultOcrPrompt;
    }

    public DtoPreferences setDefaultOcrPrompt(String defaultOcrPrompt) {
        this.defaultOcrPrompt = defaultOcrPrompt;
        return this;
    }

    public int getQwenConnectTimeout() {
        return qwenConnectTimeout;
    }

    public DtoPreferences setQwenConnectTimeout(int qwenConnectTimeout) {
        this.qwenConnectTimeout = qwenConnectTimeout;
        return this;
    }

    public int getDftQwenConnectTimeout() {
        return dftQwenConnectTimeout;
    }

    public DtoPreferences setDftQwenConnectTimeout(int dftQwenConnectTimeout) {
        this.dftQwenConnectTimeout = dftQwenConnectTimeout;
        return this;
    }

    public int getOcrMaxTokens() {
        return ocrMaxTokens;
    }

    public DtoPreferences setOcrMaxTokens(int ocrMaxTokens) {
        this.ocrMaxTokens = ocrMaxTokens;
        return this;
    }

    public int getDftQwenMaxTokens() {
        return dftQwenMaxTokens;
    }

    public DtoPreferences setDftQwenMaxTokens(int dftQwenMaxTokens) {
        this.dftQwenMaxTokens = dftQwenMaxTokens;
        return this;
    }

    public int getQwenReadTimeout() {
        return qwenReadTimeout;
    }

    public DtoPreferences setQwenReadTimeout(int qwenReadTimeout) {
        this.qwenReadTimeout = qwenReadTimeout;
        return this;
    }

    public int getDftQwenReadTimeout() {
        return dftQwenReadTimeout;
    }

    public DtoPreferences setDftQwenReadTimeout(int dftQwenReadTimeout) {
        this.dftQwenReadTimeout = dftQwenReadTimeout;
        return this;
    }

    public Set<String> getOcrModels() {
        return ocrModels;
    }

    public DtoPreferences setOcrModels(Set<String> ocrModels) {
        this.ocrModels = ocrModels;
        return this;
    }

    public String getSelectedOcrModel() {
        return selectedOcrModel;
    }

    public DtoPreferences setSelectedOcrModel(String selectedOcrModel) {
        this.selectedOcrModel = selectedOcrModel;
        return this;
    }

    public String getAgentInstructions() {
        return agentInstructions;
    }

    public DtoPreferences setAgentInstructions(String agentInstructions) {
        this.agentInstructions = agentInstructions;
        return this;
    }

    public String getDftAgentInstructions() {
        return dftAgentInstructions;
    }

    public DtoPreferences setDftAgentInstructions(String dftAgentInstructions) {
        this.dftAgentInstructions = dftAgentInstructions;
        return this;
    }
}

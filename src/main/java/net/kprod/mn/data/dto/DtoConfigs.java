package net.kprod.mn.data.dto;

import net.kprod.mn.data.entity.EntityPreferences;
import net.kprod.mn.data.entity.EntityPreferencesId;
import net.kprod.mn.data.enums.PreferenceKey;

import java.util.Arrays;
import java.util.List;

public class DtoConfigs {
    private boolean set = false;
    private boolean useDefaultPrompt;
    private String  prompt;

    private boolean useDefaultModel;
    private String model;

    private String inputFolderId;
    private String outputFolderId;

    private boolean useDefaultAiConnectTimeout;
    private int aiConnectTimeout;

    private boolean useDefaultAiReadTimeout;
    private int aiReadTimeout;

    private boolean useDefaultModelMaxTokens;
    private int modelMaxTokens;

    public List<EntityPreferences> toEntities(String username) {
        List<EntityPreferences> list = Arrays.asList(
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.set), Boolean.toString(set)),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.prompt), prompt),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.useDefaultModel), Boolean.toString(useDefaultModel)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.model), model),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.useDefaultPrompt), Boolean.toString(useDefaultPrompt)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.inputFolderId), inputFolderId),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.outputFolderId), outputFolderId),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.useDefaultAiConnectTimeout), Boolean.toString(useDefaultAiConnectTimeout)),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.aiConnectTimeout), Integer.toString(aiConnectTimeout)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.useDefaultAiReadTimeout), Boolean.toString(useDefaultAiReadTimeout)),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.aiReadTimeout), Integer.toString(aiReadTimeout)),

                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.useDefaultModelMaxTokens), Boolean.toString(useDefaultModelMaxTokens)),
                new EntityPreferences(new EntityPreferencesId(username, PreferenceKey.modelMaxTokens), Integer.toString(modelMaxTokens))
        );
        return list;
    }

    public DtoConfigs setSet(boolean set) {
        this.set = set;
        return this;
    }

    public DtoConfigs setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public DtoConfigs setUseDefaultModel(boolean useDefaultModel) {
        this.useDefaultModel = useDefaultModel;
        return this;
    }

    public DtoConfigs setModel(String model) {
        this.model = model;
        return this;
    }

    public DtoConfigs setUseDefaultPrompt(boolean useDefaultPrompt) {
        this.useDefaultPrompt = useDefaultPrompt;
        return this;
    }

    public DtoConfigs setInputFolderId(String inputFolderId) {
        this.inputFolderId = inputFolderId;
        return this;
    }

    public DtoConfigs setOutputFolderId(String outputFolderId) {
        this.outputFolderId = outputFolderId;
        return this;
    }

    public DtoConfigs setUseDefaultAiConnectTimeout(boolean useDefaultAiConnectTimeout) {
        this.useDefaultAiConnectTimeout = useDefaultAiConnectTimeout;
        return this;
    }

    public DtoConfigs setAiConnectTimeout(int aiConnectTimeout) {
        this.aiConnectTimeout = aiConnectTimeout;
        return this;
    }

    public DtoConfigs setUseDefaultAiReadTimeout(boolean useDefaultAiReadTimeout) {
        this.useDefaultAiReadTimeout = useDefaultAiReadTimeout;
        return this;
    }

    public DtoConfigs setAiReadTimeout(int aiReadTimeout) {
        this.aiReadTimeout = aiReadTimeout;
        return this;
    }

    public DtoConfigs setUseDefaultModelMaxTokens(boolean useDefaultModelMaxTokens) {
        this.useDefaultModelMaxTokens = useDefaultModelMaxTokens;
        return this;
    }

    public DtoConfigs setModelMaxTokens(int modelMaxTokens) {
        this.modelMaxTokens = modelMaxTokens;
        return this;
    }

    public boolean isSet() {
        return set;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean useDefaultModel() {
        return useDefaultModel;
    }

    public String getModel() {
        return model;
    }

    public boolean isUseDefaultPrompt() {
        return useDefaultPrompt;
    }

    public String getInputFolderId() {
        return inputFolderId;
    }

    public String getOutputFolderId() {
        return outputFolderId;
    }

    public boolean isUseDefaultAiConnectTimeout() {
        return useDefaultAiConnectTimeout;
    }

    public int getAiConnectTimeout() {
        return aiConnectTimeout;
    }

    public boolean isUseDefaultAiReadTimeout() {
        return useDefaultAiReadTimeout;
    }

    public int getAiReadTimeout() {
        return aiReadTimeout;
    }

    public boolean isUseDefaultModelMaxTokens() {
        return useDefaultModelMaxTokens;
    }

    public int getModelMaxTokens() {
        return modelMaxTokens;
    }
}

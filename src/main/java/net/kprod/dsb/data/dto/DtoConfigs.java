package net.kprod.dsb.data.dto;

import net.kprod.dsb.data.entity.EntityConfig;
import net.kprod.dsb.data.entity.EntityConfigId;
import net.kprod.dsb.data.enums.PreferenceKey;

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

    public List<EntityConfig> toEntities(String username) {
        List<EntityConfig> list = Arrays.asList(
                new EntityConfig(new EntityConfigId(username, PreferenceKey.set), Boolean.toString(set)),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.prompt), prompt),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.useDefaultModel), Boolean.toString(useDefaultModel)),

                new EntityConfig(new EntityConfigId(username, PreferenceKey.model), model),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.useDefaultPrompt), Boolean.toString(useDefaultPrompt)),

                new EntityConfig(new EntityConfigId(username, PreferenceKey.inputFolderId), inputFolderId),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.outputFolderId), outputFolderId),

                new EntityConfig(new EntityConfigId(username, PreferenceKey.useDefaultAiConnectTimeout), Boolean.toString(useDefaultAiConnectTimeout)),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.aiConnectTimeout), Integer.toString(aiConnectTimeout)),

                new EntityConfig(new EntityConfigId(username, PreferenceKey.useDefaultAiReadTimeout), Boolean.toString(useDefaultAiReadTimeout)),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.aiReadTimeout), Integer.toString(aiReadTimeout)),

                new EntityConfig(new EntityConfigId(username, PreferenceKey.useDefaultModelMaxTokens), Boolean.toString(useDefaultModelMaxTokens)),
                new EntityConfig(new EntityConfigId(username, PreferenceKey.modelMaxTokens), Integer.toString(modelMaxTokens))
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

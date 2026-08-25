package fr.monkeynotes.mn.data.enums;

public enum PreferenceKey {
    set,

    syncOption,
    //todo change this name or create another pref intended for monleySync (rootFolder id)
    inputFolderId,
    cropImage,

    remoteRootFolderPath,

    qwenConnectTimeout,
    qwenReadTimeout,
    qwenMaxTokens,
    dftQwenMaxTokens,
    dftQwenConnectTimeout,
    dftQwenReadTimeout,

    ocrPrompt,
    selectedOcrModel,

    agentInstructions,
    selectedAgentModel,
    dftAssistantInstructions;

}

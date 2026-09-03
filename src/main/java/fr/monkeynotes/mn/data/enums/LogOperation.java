package fr.monkeynotes.mn.data.enums;

public enum LogOperation {
    login,
    logout,
    savePreferences,
    upload,
    ocr,
    createTranscript,
    updateTranscript,
    passwordChanged, promoteAdmin, userNotFound, createUser, exportBackup, importBackup, sync, other
}

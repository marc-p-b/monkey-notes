package fr.monkeynotes.mn.data.dto.agent;

import fr.monkeynotes.mn.data.entity.EntityAgent;
import fr.monkeynotes.mn.data.entity.IdFile;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class DtoAgent {
    private String uuid;
    private Set<String> fileId;
    private String username;
    private String assistantId;
    private String threadId;

    public static Set<String> createIdFilesSet(String fileIds, String username) {
        return Arrays.stream(fileIds.trim().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public static DtoAgent of(EntityAgent agent) {
        return new DtoAgent()
                .setUuid(agent.getUuid())
                .setFileId(createIdFilesSet(agent.getFileIds(), agent.getUsername()))
                .setAssistantId(agent.getAssistantId())
                .setThreadId(agent.getThreadId());
    }

    public String getUuid() {
        return uuid;
    }

    public DtoAgent setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public Set<String> getFileId() {
        return fileId;
    }

    public DtoAgent setFileId(Set<String> fileId) {
        this.fileId = fileId;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DtoAgent setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getAssistantId() {
        return assistantId;
    }

    public DtoAgent setAssistantId(String assistantId) {
        this.assistantId = assistantId;
        return this;
    }

    public String getThreadId() {
        return threadId;
    }

    public DtoAgent setThreadId(String threadId) {
        this.threadId = threadId;
        return this;
    }
}

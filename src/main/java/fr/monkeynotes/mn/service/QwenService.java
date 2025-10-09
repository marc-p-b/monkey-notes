package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.CompletionResponse;

import java.net.URL;

public interface QwenService {
    CompletionResponse analyzeImage(String fileId, URL imageURL);
}

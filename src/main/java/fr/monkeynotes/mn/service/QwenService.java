package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.CompletionResponse;

import java.net.URL;

public interface QwenService {
    CompletionResponse analyzeImage(String fileId, URL imageURL);
//    CompletionResponse analyzeImage(String user, String fileId, int imageNum, String model, String prompt);
}

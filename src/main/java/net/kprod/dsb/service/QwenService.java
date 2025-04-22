package net.kprod.dsb.service;

import net.kprod.dsb.data.CompletionResponse;

import java.net.URL;

public interface QwenService {
    CompletionResponse analyzeImage(String fileId, URL imageURL);
    //CompletionResponse analyzeImage(String fileId, URL imageURL, String model, String prompt);
}

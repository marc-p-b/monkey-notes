package net.kprod.mn.service;

import net.kprod.mn.data.CompletionResponse;

import java.net.URL;

public interface QwenService {
    CompletionResponse analyzeImage(String fileId, URL imageURL);
}

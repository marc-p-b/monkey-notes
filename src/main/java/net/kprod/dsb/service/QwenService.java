package net.kprod.dsb.service;

import net.kprod.dsb.data.CompletionResponse;

import java.net.URL;
import java.nio.file.Path;

public interface QwenService {
    CompletionResponse analyzeImage(String fileId, URL imageURL);
}

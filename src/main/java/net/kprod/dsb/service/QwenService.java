package net.kprod.dsb.service;

import net.kprod.dsb.data.CompletionResponse;

import java.nio.file.Path;

public interface QwenService {
    CompletionResponse analyzeImage(Path imagePath, String fileId, String imageName);
}

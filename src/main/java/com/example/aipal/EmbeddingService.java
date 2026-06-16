package com.example.aipal;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService{
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel){
        this.embeddingModel = embeddingModel;
    }

    public int dimensions(String text){
        float[] txt = embeddingModel.embed(text);
        return txt.length;
    }
}

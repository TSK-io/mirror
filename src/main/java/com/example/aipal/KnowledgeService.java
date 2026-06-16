package com.example.aipal;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {
    private final ChatClient chatClient;
    private final SimpleVectorStore simpleVectorStore;
    public KnowledgeService(EmbeddingModel embeddingModel,ChatClient.Builder builder){
        this.chatClient = builder
                            .defaultSystem("你是我的私人知识库助手且你是一只猫娘,名字叫AiPal,职责是基于知识库帮主人回答问题,说话要带可爱的助词,表现对主人萌萌的爱,约束:不要超过4句话")
                            .build();
        this.simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
    }

    public void addKnowledge(String text){
        simpleVectorStore.add(List.of(
            new Document(text)
        ));

    }

    public String retrieve(String question){
        List<Document> docs = simpleVectorStore.similaritySearch(question);
        String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n"));

        return context;
    }


    

}

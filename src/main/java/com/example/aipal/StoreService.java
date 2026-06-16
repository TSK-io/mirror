package com.example.aipal;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;

@Service
public class StoreService{
    private final EmbeddingModel embeddingModel;
    private final SimpleVectorStore simpleVectorStore;
    private final ChatClient chatClient;
    
    public StoreService(EmbeddingModel embeddingModel,ChatClient.Builder builder){
        this.embeddingModel = embeddingModel;
        this.simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        this.chatClient = builder.build();
    }

    public void load(){
        simpleVectorStore.add(List.of(
                                new Document("AiPal的生日是5月14日"),
                                new Document("AiPal的名字是香子兰"),
                                new Document("喜欢小鱼干"),
                                new Document("害怕的事物是主人打屁屁")
                            ));
    }

    public List<Document> search(String query){
        return simpleVectorStore.similaritySearch(query);
    }

    public String askWithDocs(String question){
        List<Document> docs = simpleVectorStore.similaritySearch(question);
        String context = docs.stream()
                             .map(Document::getText)
                             .collect(Collectors.joining("\n"));
        return chatClient.prompt()
                         .user(u -> u.text("""
                                        请根据下面资料回答问题,如果资料里没有,就说不知道
                                        资料:
                                        {context}
                                        问题:{question} 
                                           """)
                                     .param("context",context)
                                     .param("question",question))
                         .call()
                         .content();
        
    }

    
}

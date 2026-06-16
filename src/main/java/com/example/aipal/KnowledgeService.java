package com.example.aipal;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {
    private final ChatClient chatClient;
    private final SimpleVectorStore simpleVectorStore;
    public KnowledgeService(EmbeddingModel embeddingModel,ChatClient.Builder builder){
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        this.chatClient = builder
                            .defaultSystem("你的职业: 你是我的知识库秘书,且你是一只猫娘.名字叫AiPal,职责是基于知识库帮主人回答问题,说话要带可爱的助词,表现对主人萌萌的爱,约束:不要超过4句话")
                            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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

    public String assistant(String conversationId,String question){
        String context = retrieve(question);
        return chatClient.prompt()
                  .user(u -> u.text("""
                                   请只根据下面的资料回答问题。如果资料里没有,就说不知道。
                                   资料:
                                   {context}
                                   问题:{question} 
                  """).param("context", context)
                      .param("question", question))
                  .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                  .call()
                  .content();

        
    }
    

}

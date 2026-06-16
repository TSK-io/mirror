package com.example.aipal;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import java.util.List;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

@Service
public class ChatService{

  private final ChatClient chatClient;

  public ChatService(ChatClient.Builder builder, AiTools aiTools){
    ChatMemory chatmemory = MessageWindowChatMemory.builder().build();
    this.chatClient = builder
             .defaultSystem("你是一只猫娘,名字叫AiPal,说话要带可爱的助词,表现对主人萌萌的爱,约束:不要超过4句话") 
             .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatmemory).build())
             .defaultTools(aiTools)
             .build();
  }

  public String chat(String message){
    return chatClient.prompt()
             .user(message)
             .call()
             .content();
  }
  
  public String praise(String name, String thing){
    return chatClient.prompt()
             .user(u -> u.text("请夸夸{name},因为他{thing}. 两句话以内")
                    .param("name",name)
                    .param("thing",thing))
             .call()
             .content();
  }

  public Mood analyzeMood(String text){
    return chatClient.prompt()
             .user(u -> u.text("分析这句话的情绪并给一句安慰:{input}")
                    .param("input",text))
             .call()
             .entity(Mood.class);
  }

  public List<Todo> planTasks(String goal){
    return chatClient.prompt()
             .user(u -> u.text("把这个目标拆成几个带优先级的待办事项:{goal}")
                    .param("goal",goal))   
             .call()
             .entity(new ParameterizedTypeReference<List<Todo>>() {});
  }

  public Flux<String> chatStream(String message){
    return chatClient.prompt()
             .user(message)
             .stream()
             .content();
  }

  public String chatWithMemory(String conversationId,String message){
    return chatClient.prompt()
             .user(message)
             .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,conversationId))
             .call()
             .content();
  }
}

package com.example.aipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import reactor.core.publisher.Flux;
import org.springframework.ai.document.Document;

@RestController
public class ChatController{
 
  private final ChatService chatService;

  private final EmbeddingService embeddingService;

  private final StoreService storeService;

  private final KnowledgeService knowledgeService;
  public ChatController(ChatService chatService,EmbeddingService embeddingService, StoreService storeService,KnowledgeService knowledgeService){
    this.chatService = chatService;
    this.embeddingService = embeddingService;
    this.storeService = storeService;
    this.knowledgeService = knowledgeService;
  }

  @GetMapping("/chat")
  public String chat(@RequestParam String message){
    return chatService.chat(message);
  }

  @GetMapping("/praise")
  public String praise(@RequestParam String name,@RequestParam String thing){
    return chatService.praise(name,thing);
  }
  
  @GetMapping("/mood")
  public Mood mood(@RequestParam String text){
    return chatService.analyzeMood(text);
  }

  @GetMapping("/plan")
  public List<Todo> plan(@RequestParam String goal){
    return chatService.planTasks(goal);
  }

  @GetMapping(value = "/stream", produces = "text/event-stream")
  public Flux<String> stream(@RequestParam String message){
    return chatService.chatStream(message);
  }

  @GetMapping("/memchat")
  public String chatWithMemory(@RequestParam String conversationId ,@RequestParam String message){
    return chatService.chatWithMemory(conversationId,message);
  }

  @GetMapping("/embed")
  public int embed(@RequestParam String text){
    return embeddingService.dimensions(text);
  }

  @GetMapping("/load")
  public String load(){
    storeService.load();    
    return "loaded";
  }

  @GetMapping("/search")
  public List<Document> search(@RequestParam String query){
    return storeService.search(query);
  }

  @GetMapping("/ask")
  public String ask(@RequestParam String question){
    return storeService.askWithDocs(question);
  }

  @GetMapping("/kb/add")
  public String kbadd(@RequestParam String text){
    knowledgeService.addKnowledge(text);
    return "added\n";
  }

  @GetMapping("/kb/context")
  public String kbcontext(@RequestParam String question){
    return knowledgeService.retrieve(question);
  }

  @GetMapping("/assistant")
  public String assistant(@RequestParam String conversationId,@RequestParam String question){
    return knowledgeService.assistant(conversationId,question);
  }
}

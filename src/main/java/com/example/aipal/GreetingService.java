package com.example.aipal;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class GreetingService{

  @Value("${aipal.name}")
  private String name;

  public String greet(){
    return "你好,我是 " + name + "!";
  }
}

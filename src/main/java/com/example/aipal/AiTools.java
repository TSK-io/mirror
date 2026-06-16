package com.example.aipal;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class AiTools{

    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime(){
        return LocalDateTime.now().toString();
    }

    @Tool(description = "计算两个整数相加的结果")
    public int add(int a, int b){
        return a+b;
    }
}

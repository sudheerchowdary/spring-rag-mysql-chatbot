package com.rag.mysql.rag_mysql_chatbot.controller;

import com.rag.mysql.rag_mysql_chatbot.service.LLMSQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private LLMSQLService llmSqlService;
    @PostMapping(produces = "text/plain; charset=UTF-8")
    public String sendMessage(@RequestBody String request) {
        return llmSqlService.queryDatabase(request);
    }
}

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

    @PostMapping(
        consumes = {"text/plain", "application/json"},
        produces = "text/plain; charset=UTF-8"
    )
    public String sendMessage(@RequestBody String request) {
        // Handle both plain text and JSON input
        String question = request;
        if (request.startsWith("{") && request.contains("message")) {
            // JSON format: extract message field
            try {
                question = request.replaceAll(".*\"message\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            } catch (Exception e) {
                question = request;
            }
        }
        return llmSqlService.queryDatabase(question);
    }
}

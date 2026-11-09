package com.travelai.agent.backend.travelaiagentbackend.controllers;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelai.agent.backend.travelaiagentbackend.services.ChatService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/chat")
@RequiredArgsConstructor 
public class ChatController {
    
    private final ChatService chatService;

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> chatGet(@RequestHeader("Conversation-Id") String conversationId,
            @RequestBody String message) {
        return chatService.chat(message, conversationId)
                .thenApply(ResponseEntity::ok);
    }

}

package com.travelai.agent.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.travelai.agent.backend.services.ChatService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/chat")
@RequiredArgsConstructor 
public class ChatController {
    
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<String> chatGet(@RequestHeader("Conversation-Id") String conversationId,
            @RequestBody String message) {
        return ResponseEntity.ok(chatService.chat(message, conversationId));
    }

}

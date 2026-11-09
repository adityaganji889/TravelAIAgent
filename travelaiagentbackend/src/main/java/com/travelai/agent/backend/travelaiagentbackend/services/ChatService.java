package com.travelai.agent.backend.travelaiagentbackend.services;

import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    @Async("chatExecutor")
    public CompletableFuture<String> chat(String query, String conversationId) {
        // Use the chatClient to send the query and get a response
        String response = chatClient
                .prompt().user(query)
                .advisors(a ->
                a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().content();
        return CompletableFuture.completedFuture(response);
    }

}
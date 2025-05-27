package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.service.ChatService;
import com.example.demo.service.WebSocketService;

import jakarta.annotation.Nonnull;

/**
 * Controller for chat functionality.
 */
@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * Handles new chat message submission.
     *
     * @param message the chat message to send
     * @return the created message
     */
    @PostMapping("/api/messages")
    @ResponseBody
    public ChatMessageDto sendMessage(@RequestBody @Nonnull ChatMessageDto message) {
        ChatMessageDto created = chatService.createMessage(message);
        webSocketService.broadcastMessage();
        return created;
    }

    /**
     * Gets the 50 most recent chat messages.
     * Returns DTOs to provide a clean API interface.
     *
     * @return list of recent messages as DTOs
     */
    @GetMapping("/api/messages/recent")
    @ResponseBody
    public @Nonnull List<ChatMessageDto> getRecentMessages() {
        return chatService.getRecentMessagesAsDto();
    }

    /**
     * Fetch older chat messages before a specific message ID.
     *
     * @param beforeId the ID to fetch messages before
     * @param limit    maximum number of messages to return (default 20)
     * @return list of messages in chronological order (oldest first)
     */
    @GetMapping("/api/messages")
    @ResponseBody
    public @Nonnull List<ChatMessageDto> getMessagesBefore(
            @RequestParam("beforeId") @Nonnull Long beforeId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return chatService.getMessagesBeforeAsDto(beforeId, limit);
    }
}

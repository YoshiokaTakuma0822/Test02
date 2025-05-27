package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;

@Service
public class MessageService {

    public ChatMessage createJoinMessage(User user) {
        ChatMessage message = new ChatMessage();
        message.setContent(user.getName() + " joined");
        message.setSender(user);
        message.setType(ChatMessage.MessageType.JOIN);
        return message;
    }

    public ChatMessage createLeaveMessage(User user) {
        ChatMessage message = new ChatMessage();
        message.setContent(user.getName() + " left");
        message.setSender(user);
        message.setType(ChatMessage.MessageType.LEAVE);
        return message;
    }
}

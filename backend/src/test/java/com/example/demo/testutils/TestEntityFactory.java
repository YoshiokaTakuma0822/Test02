package com.example.demo.testutils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;

/**
 * Factory class for creating test entities and DTOs.
 */
public class TestEntityFactory {

    public static User createUser(String name, String email) {
        User user = new User(name, email);
        // Use reflection to set ID since it's a private field
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L); // Default ID
            idField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
        return user;
    }

    public static User createUser(String name, String email, Long id) {
        User user = createUser(name, email);
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
            idField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID field", e);
        }
        return user;
    }

    public static UserDto createUserDto(Long id, String name, String email) {
        return new UserDto(id, name, email);
    }

    public static ChatMessage createChatMessage(Long id, String content, User sender, ChatMessage.MessageType type) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setContent(content);
        message.setSender(sender);
        message.setType(type);
        message.setCreatedAt(OffsetDateTime.of(2025, 5, 20, 10, 0, 0, 0, ZoneOffset.of("+09:00")));
        return message;
    }

    public static ChatMessageDto createChatMessageDto(Long id, UserDto sender, String content, OffsetDateTime createdAt,
            String type) {
        return new ChatMessageDto(id, sender, content, createdAt, type);
    }
}

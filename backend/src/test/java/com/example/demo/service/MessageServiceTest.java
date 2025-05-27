package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @InjectMocks
    private MessageService messageService;

    @Test
    @DisplayName("JOINメッセージが正しく生成されること")
    void createJoinMessage_ShouldCreateCorrectMessage() {
        // Given
        User user = new User("Test User", "test@example.com");

        // When
        ChatMessage result = messageService.createJoinMessage(user);

        // Then
        assertThat(result.getContent()).isEqualTo("Test User joined");
        assertThat(result.getSender()).isEqualTo(user);
        assertThat(result.getType()).isEqualTo(ChatMessage.MessageType.JOIN);
    }

    @Test
    @DisplayName("LEAVEメッセージが正しく生成されること")
    void createLeaveMessage_ShouldCreateCorrectMessage() {
        // Given
        User user = new User("Test User", "test@example.com");

        // When
        ChatMessage result = messageService.createLeaveMessage(user);

        // Then
        assertThat(result.getContent()).isEqualTo("Test User left");
        assertThat(result.getSender()).isEqualTo(user);
        assertThat(result.getType()).isEqualTo(ChatMessage.MessageType.LEAVE);
    }
}

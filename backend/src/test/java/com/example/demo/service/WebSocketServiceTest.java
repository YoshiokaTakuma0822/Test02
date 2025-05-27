package com.example.demo.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private ActiveUsersService activeUsersService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private WebSocketService webSocketService;

    @Test
    void handleUserConnect_WithValidUser() {
        // Arrange
        String sessionId = "session-1";
        Long userId = 123L;
        User user = new User("test", "test@example.com");
        ChatMessage joinMessage = new ChatMessage();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageService.createJoinMessage(user)).thenReturn(joinMessage);

        // Act
        webSocketService.handleUserConnect(sessionId, userId);

        // Assert
        verify(activeUsersService).addUser(user);
        verify(messagingTemplate).convertAndSend("/topic/chat.users.update", "");
        verify(chatService).createMessageFromEntity(joinMessage);
        verify(messagingTemplate).convertAndSend("/topic/chat.messages.update", "");
    }

    @Test
    void handleUserDisconnect_WithValidUser() {
        // Arrange
        String sessionId = "session-1";
        Long userId = 123L;
        User user = new User("test", "test@example.com");
        ChatMessage joinMessage = new ChatMessage();
        ChatMessage leaveMessage = new ChatMessage();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageService.createJoinMessage(user)).thenReturn(joinMessage);
        when(messageService.createLeaveMessage(user)).thenReturn(leaveMessage);

        // Act
        webSocketService.handleUserConnect(sessionId, userId);
        webSocketService.handleUserDisconnect(sessionId);

        // Assert - should verify both join and leave notifications
        verify(chatService).createMessageFromEntity(joinMessage); // For join
        verify(chatService).createMessageFromEntity(leaveMessage); // For leave
        verify(messagingTemplate, times(2)).convertAndSend("/topic/chat.messages.update", "");
    }

    @Test
    void handleUserSubscribe_ShouldLogSubscription() {
        // Arrange
        String sessionId = "session-1";
        Long userId = 123L;
        String destination = "/topic/chat.messages";

        // Act
        webSocketService.handleUserConnect(sessionId, userId); // まず接続
        webSocketService.handleUserSubscribe(sessionId, destination);
    }

    @Test
    void onMessage_ShouldBroadcastMessage() {
        // Arrange
        byte[] messageBytes = "test".getBytes();
        byte[] patternBytes = "pattern".getBytes();
        Message message = new DefaultMessage(patternBytes, messageBytes);

        // Act
        webSocketService.onMessage(message, patternBytes);

        // Assert
        verify(messagingTemplate).convertAndSend("/topic/chat.messages.update", "");
    }
}

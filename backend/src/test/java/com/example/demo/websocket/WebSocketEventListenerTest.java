package com.example.demo.websocket;

import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.demo.service.WebSocketService;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    @Test
    void handleWebSocketConnectListener_WithValidUserId() {
        // Arrange
        Map<String, Object> messageHeaders = new HashMap<>();
        messageHeaders.put("nativeHeaders", Map.of("userId", List.of("123")));
        messageHeaders.put("simpSessionId", "session-1");
        Message<byte[]> testMessage = new GenericMessage<>(new byte[0], messageHeaders);
        SessionConnectEvent event = new SessionConnectEvent(this, testMessage);

        // Act
        webSocketEventListener.handleWebSocketConnectListener(event);

        // Assert
        verify(webSocketService).handleUserConnect("session-1", 123L);
    }

    @Test
    void handleSessionConnected_WithValidUserId() {
        // Arrange
        Map<String, Object> messageHeaders = new HashMap<>();
        messageHeaders.put("simpSessionId", "session-1");
        messageHeaders.put("simpSessionAttributes", Map.of("userId", 123L));
        Message<byte[]> testMessage = new GenericMessage<>(new byte[0], messageHeaders);
        SessionConnectedEvent event = new SessionConnectedEvent(this, testMessage);

        // Act
        webSocketEventListener.handleSessionConnected(event);

        // Assert
        verify(webSocketService).handleUserConnect("session-1", 123L);
    }

    @Test
    void handleWebSocketDisconnectListener_ShouldDelegateToService() {
        // Arrange
        String sessionId = "session-1";
        Message<byte[]> testMessage = new GenericMessage<>(new byte[0]);
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, testMessage, sessionId, null);

        // Act
        webSocketEventListener.handleWebSocketDisconnectListener(event);

        // Assert
        verify(webSocketService).handleUserDisconnect(sessionId);
    }
}

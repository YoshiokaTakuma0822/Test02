package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.dto.UserDto;
import com.example.demo.service.ChatService;
import com.example.demo.service.WebSocketService;
import com.example.demo.testutils.TestEntityFactory;

/**
 * WebSocketを介したチャット機能のテストケース
 */
@ExtendWith(MockitoExtension.class)
public class ChatControllerWebSocketTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private ChatController chatController;

    private UserDto testUser;
    private ChatMessageDto testMessage;

    @BeforeEach
    void setUp() {
        // テストユーザーの作成
        testUser = TestEntityFactory.createUserDto(1L, "John Doe", "john@example.com");

        // テストメッセージの作成
        testMessage = TestEntityFactory.createChatMessageDto(
                1L,
                testUser,
                "Hello, everyone!",
                java.time.OffsetDateTime.now(),
                "CHAT");
    }

    @Test
    @DisplayName("新規メッセージの送信でWebSocketを通じて通知されること")
    void testSendMessage() {
        // Given
        when(chatService.createMessage(any(ChatMessageDto.class))).thenReturn(testMessage);

        // When
        ChatMessageDto result = chatController.sendMessage(testMessage);

        // Then
        assertThat(result).isEqualTo(testMessage);
        verify(chatService).createMessage(testMessage);
        verify(webSocketService).broadcastMessage();
    }

    @Test
    @DisplayName("JOIN時にWebSocketを通じて参加通知が送信されること")
    void testUserJoin() {
        // Given
        ChatMessageDto joinMessage = TestEntityFactory.createChatMessageDto(
                2L,
                testUser,
                "John Doe joined",
                java.time.OffsetDateTime.now(),
                "JOIN");
        when(chatService.createMessage(any(ChatMessageDto.class))).thenReturn(joinMessage);

        // When
        ChatMessageDto result = chatController.sendMessage(joinMessage);

        // Then
        assertThat(result).isEqualTo(joinMessage);
        verify(chatService).createMessage(joinMessage);
        verify(webSocketService).broadcastMessage();
    }

    @Test
    @DisplayName("LEAVE時にWebSocketを通じて退出通知が送信されること")
    void testUserLeave() {
        // Given
        ChatMessageDto leaveMessage = TestEntityFactory.createChatMessageDto(
                3L,
                testUser,
                "John Doe left",
                java.time.OffsetDateTime.now(),
                "LEAVE");
        when(chatService.createMessage(any(ChatMessageDto.class))).thenReturn(leaveMessage);

        // When
        ChatMessageDto result = chatController.sendMessage(leaveMessage);

        // Then
        assertThat(result).isEqualTo(leaveMessage);
        verify(chatService).createMessage(leaveMessage);
        verify(webSocketService).broadcastMessage();
    }

    @Test
    @DisplayName("エラー発生時に適切に処理されること")
    void testHandleError() {
        // Given
        when(chatService.createMessage(any(ChatMessageDto.class)))
                .thenThrow(new RuntimeException("Test error"));

        // When & Then
        try {
            chatController.sendMessage(testMessage);
        } catch (RuntimeException e) {
            assertThat(e).hasMessage("Test error");
        }
    }
}

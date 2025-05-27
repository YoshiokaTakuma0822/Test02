package com.example.demo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.dto.UserDto;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ChatService;
import com.example.demo.service.WebSocketService;
import com.example.demo.testutils.TestEntityFactory;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ChatMessageRepository chatMessageRepository;

    @MockitoBean
    private WebSocketService webSocketService;

    private List<ChatMessageDto> messageDtos;
    private ChatMessageDto messageDto1;
    private ChatMessageDto messageDto2;
    private UserDto userDto1;
    private UserDto userDto2;

    @BeforeEach
    void setUp() {
        // テストユーザーの作成
        userDto1 = TestEntityFactory.createUserDto(1L, "John Doe", "john@example.com");
        userDto2 = TestEntityFactory.createUserDto(2L, "Jane Smith", "jane@example.com");

        // テストメッセージの作成
        messageDto1 = TestEntityFactory.createChatMessageDto(
                1L,
                userDto1,
                "Hello, everyone!",
                OffsetDateTime.of(2025, 5, 20, 10, 0, 0, 0, ZoneOffset.of("+09:00")),
                "CHAT");

        messageDto2 = TestEntityFactory.createChatMessageDto(
                2L,
                userDto2,
                "Hi John!",
                OffsetDateTime.of(2025, 5, 20, 10, 5, 0, 0, ZoneOffset.of("+09:00")),
                "CHAT");

        messageDtos = Arrays.asList(messageDto1, messageDto2);
    }

    @Nested
    @DisplayName("GET /api/messages/recent のテスト")
    class GetRecentMessagesTests {

        @Test
        @DisplayName("メッセージが存在する場合、すべてのメッセージを返すこと")
        void shouldReturnAllMessagesWhenExist() throws Exception {
            when(chatService.getRecentMessagesAsDto()).thenReturn(messageDtos);

            mockMvc.perform(get("/api/messages/recent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].content", is("Hello, everyone!")))
                    .andExpect(jsonPath("$[0].sender.id", is(1)))
                    .andExpect(jsonPath("$[0].sender.name", is("John Doe")))
                    .andExpect(jsonPath("$[0].type", is("CHAT")))
                    .andExpect(jsonPath("$[0].createdAt", notNullValue()))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].content", is("Hi John!")))
                    .andExpect(jsonPath("$[1].sender.id", is(2)))
                    .andExpect(jsonPath("$[1].sender.name", is("Jane Smith")))
                    .andExpect(jsonPath("$[1].type", is("CHAT")))
                    .andExpect(jsonPath("$[1].createdAt", notNullValue()));

            verify(chatService, times(1)).getRecentMessagesAsDto();
        }

        @Test
        @DisplayName("メッセージが存在しない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            when(chatService.getRecentMessagesAsDto()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/messages/recent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(chatService, times(1)).getRecentMessagesAsDto();
        }

        @Test
        @DisplayName("送信者がnullのメッセージを正しく処理できること")
        void shouldHandleMessagesWithNullSender() throws Exception {
            ChatMessageDto messageWithNullSender = TestEntityFactory.createChatMessageDto(
                    3L,
                    null,
                    "System message",
                    OffsetDateTime.of(2025, 5, 20, 10, 10, 0, 0, ZoneOffset.of("+09:00")),
                    "JOIN");

            when(chatService.getRecentMessagesAsDto()).thenReturn(Collections.singletonList(messageWithNullSender));

            mockMvc.perform(get("/api/messages/recent"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(3)))
                    .andExpect(jsonPath("$[0].content", is("System message")))
                    .andExpect(jsonPath("$[0].sender").doesNotExist())
                    .andExpect(jsonPath("$[0].createdAt", notNullValue()))
                    .andExpect(jsonPath("$[0].type", is("JOIN")));

            verify(chatService, times(1)).getRecentMessagesAsDto();
        }

        @Test
        @DisplayName("サービスがエラーを投げた場合、500エラーを返すこと")
        void shouldReturn500WhenServiceThrowsException() throws Exception {
            when(chatService.getRecentMessagesAsDto()).thenThrow(new RuntimeException("Service error"));

            mockMvc.perform(get("/api/messages/recent"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(chatService, times(1)).getRecentMessagesAsDto();
        }
    }
}

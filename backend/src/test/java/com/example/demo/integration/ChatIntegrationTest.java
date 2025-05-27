package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.WebSocketService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private WebSocketService webSocketService;

    private @NonNull List<ChatMessageDto> getRecentMessages() {
        ResponseEntity<@NonNull List<@NonNull ChatMessageDto>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/messages/recent",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<@NonNull List<@NonNull ChatMessageDto>>() {
                });

        if (response.getStatusCode() == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Expected successful response but got: " + response.getStatusCode());
        }

        List<@NonNull ChatMessageDto> messages = response.getBody();
        if (messages == null) {
            throw new AssertionError("Response body should not be null");
        }

        return messages;
    }

    private User createTestUser() {
        User user = new User("Test User", "test@example.com");
        return userRepository.save(user);
    }

    private ChatMessage createTestMessage(User sender, String content) {
        ChatMessage message = new ChatMessage();
        message.setContent(content);
        message.setSender(sender);
        message.setType(ChatMessage.MessageType.CHAT);
        message.setCreatedAt(OffsetDateTime.now());
        return chatMessageRepository.save(message);
    }

    @Test
    @Sql(scripts = { "/test-data.sql" })
    void shouldRetrieveRecentMessages() {
        @NonNull
        List<@NonNull ChatMessageDto> messages = getRecentMessages();
        assertThat(messages).hasSize(20);
    }

    @Test
    @Sql(scripts = { "/test-data.sql" })
    void shouldCreateAndRetrieveMessage() {
        // Given
        User user = createTestUser();
        String sessionId = "test-session-" + user.getId();
        webSocketService.handleUserConnect(sessionId, user.getId());
        createTestMessage(user, "Test message");

        // When
        @NonNull
        List<@NonNull ChatMessageDto> messages = getRecentMessages();

        // Then
        assertThat(messages).hasSize(20);
        assertThat(messages.get(0).content()).isEqualTo("Test message");
        assertThat(messages.get(0).sender().name()).isEqualTo("Test User");

        // Cleanup
        webSocketService.handleUserDisconnect(sessionId);
    }

    @Test
    @Sql(scripts = { "/test-data.sql" })
    void shouldRetrieveMessagesBeforeId() {
        // When: fetch messages before ID 2 with limit 1
        ResponseEntity<@NonNull List<@NonNull ChatMessageDto>> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/messages?beforeId=2&limit=1",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<@NonNull List<@NonNull ChatMessageDto>>() {
                });

        if (response.getStatusCode() == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("Expected successful response but got: " + response.getStatusCode());
        }

        List<@NonNull ChatMessageDto> messages = response.getBody();
        if (messages == null) {
            throw new AssertionError("Response body should not be null");
        }

        // Then: only the message with ID 1 should be returned
        assertThat(messages).hasSize(1);
        @NonNull
        ChatMessageDto message = messages.get(0);
        assertThat(message.id()).isEqualTo(1L);
        assertThat(message.content()).isEqualTo("Hello, everyone!");
    }
}

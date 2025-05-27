package com.example.demo;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatMessage.MessageType;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ChatService;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        // Tests will run with existing database data
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testLazyLoadingInGetRecentMessages() {
        // Arrange: create and save a user and a chat message with unique content
        User user = new User("testuser", "test@example.com");
        user = userRepository.save(user);
        String uniqueContent = "Hello world " + System.currentTimeMillis();
        ChatMessage message = new ChatMessage();
        message.setSender(user);
        message.setContent(uniqueContent);
        message.setType(MessageType.CHAT);
        chatMessageRepository.save(message);

        // Act: get messages as DTOs
        List<ChatMessageDto> dtos = chatService.getRecentMessagesAsDto();

        // Assert: verify that our new message exists in the results and has correct
        // properties
        ChatMessageDto newMessage = dtos.stream()
                .filter(dto -> dto.content().equals(uniqueContent))
                .findFirst()
                .orElseThrow(() -> new AssertionError("新しく追加したメッセージが見つかりません"));

        Assertions.assertEquals("testuser", newMessage.sender().name());
        Assertions.assertEquals(MessageType.CHAT.toString(), newMessage.type());
    }
}

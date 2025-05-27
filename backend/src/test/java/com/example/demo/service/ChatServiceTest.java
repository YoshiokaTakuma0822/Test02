package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;
import com.example.demo.projection.ChatMessageProjection;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User user1;
    private User user2;
    private ChatMessage message1;
    private ChatMessage message2;

    @BeforeEach
    void setUp() {
        // テストユーザーの作成
        user1 = new User("John Doe", "john@example.com");
        user1.setId(1L);

        user2 = new User("Jane Smith", "jane@example.com");
        user2.setId(2L);

        // テストメッセージの作成
        message1 = new ChatMessage();
        message1.setId(1L);
        message1.setContent("Hello, everyone!");
        message1.setSender(user1);
        message1.setCreatedAt(OffsetDateTime.of(2025, 5, 20, 10, 0, 0, 0, ZoneOffset.of("+09:00")));
        message1.setType(ChatMessage.MessageType.CHAT);

        message2 = new ChatMessage();
        message2.setId(2L);
        message2.setContent("Hi John!");
        message2.setSender(user2);
        message2.setCreatedAt(OffsetDateTime.of(2025, 5, 20, 10, 5, 0, 0, ZoneOffset.of("+09:00")));
        message2.setType(ChatMessage.MessageType.CHAT);
    }

    @Test
    @DisplayName("最新のメッセージをDTOとして取得できること")
    void getRecentMessagesAsDto_ShouldReturnLatestMessages() {
        // Given
        when(chatMessageRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 20)))
                .thenReturn(Arrays.asList(createMockProjection(message1), createMockProjection(message2)));

        // When
        List<ChatMessageDto> dtos = chatService.getRecentMessagesAsDto();

        // Then
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(0).content()).isEqualTo("Hello, everyone!");
        assertThat(dtos.get(0).sender().id()).isEqualTo(1L);
        assertThat(dtos.get(0).sender().name()).isEqualTo("John Doe");
        assertThat(dtos.get(0).type()).isEqualTo("CHAT");
    }

    @Test
    @DisplayName("指定ID以前のメッセージをDTOで取得できること")
    void getMessagesBeforeAsDto_ShouldReturnMessagesBeforeIdInChronologicalOrder() {
        // Given: repository returns messages in descending order by createdAt
        when(chatMessageRepository.findByIdLessThanOrderByCreatedAtDescIdDesc(2L, PageRequest.of(0, 10)))
                .thenReturn(Arrays.asList(createMockProjection(message2), createMockProjection(message1)));

        // When
        List<ChatMessageDto> dtos = chatService.getMessagesBeforeAsDto(2L, 10);

        // Then: service should reverse list to chronological (oldest first)
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).id()).isEqualTo(1L);
        assertThat(dtos.get(1).id()).isEqualTo(2L);
    }

    private ChatMessageProjection createMockProjection(ChatMessage message) {
        return new ChatMessageProjection() {
            @Override
            public Long getId() {
                return message.getId();
            }

            @Override
            public String getContent() {
                return message.getContent();
            }

            @Override
            public OffsetDateTime getCreatedAt() {
                return message.getCreatedAt();
            }

            @Override
            public String getType() {
                return message.getType().toString();
            }

            @Override
            public UserProjection getSender() {
                return new UserProjection() {
                    @Override
                    public Long getId() {
                        return message.getSender().getId();
                    }

                    @Override
                    public String getName() {
                        return message.getSender().getName();
                    }

                    @Override
                    public String getEmail() {
                        return message.getSender().getEmail();
                    }
                };
            }
        };
    }
}

package com.example.demo.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ChatMessageDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.User;
import com.example.demo.projection.ChatMessageProjection;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;

import jakarta.annotation.Nonnull;

/**
 * Service class for handling chat-related business logic.
 */
@Service
public class ChatService {

    @Autowired
    private ActiveUsersService activeUsersService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * Creates a new chat message.
     *
     * @param messageDto the message to create
     * @return the created message as DTO
     */
    @Transactional
    public @Nonnull ChatMessageDto createMessage(@Nonnull ChatMessageDto messageDto) {
        // Create message entity
        ChatMessage message = new ChatMessage();
        message.setContent(messageDto.content());
        message.setType(ChatMessage.MessageType.valueOf(messageDto.type()));

        // Find and set sender
        User sender = userRepository.findById(messageDto.sender().id())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        message.setSender(sender);

        // Save and convert to DTO
        chatMessageRepository.save(message);
        return toDto(message);
    }

    /**
     * Creates a new chat message from an entity.
     *
     * @param message the message entity to create
     * @return the created message as DTO
     */
    @Transactional
    public @Nonnull ChatMessageDto createMessageFromEntity(@Nonnull ChatMessage message) {
        chatMessageRepository.save(message);
        return toDto(message);
    }

    /**
     * Gets the 20 most recent chat messages using projection for better
     * performance.
     *
     * @return list of recent messages as DTOs
     */
    @Transactional(readOnly = true)
    public @Nonnull List<ChatMessageDto> getRecentMessagesAsDto() {
        return chatMessageRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 20))
                .stream()
                .map(this::projectionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets the list of currently active users.
     *
     * @return list of active users as DTOs
     */
    @Transactional(readOnly = true)
    public @Nonnull List<UserDto> getActiveUsersAsDto() {
        return activeUsersService.getActiveUsers();
    }

    /**
     * Fetch messages older than the given ID and convert to DTOs, ordered
     * oldest-first.
     */
    @Transactional(readOnly = true)
    public @Nonnull List<ChatMessageDto> getMessagesBeforeAsDto(@Nonnull Long beforeId, int limit) {
        List<ChatMessageProjection> msgs = chatMessageRepository.findByIdLessThanOrderByCreatedAtDescIdDesc(beforeId,
                PageRequest.of(0, limit));
        Collections.reverse(msgs);
        return msgs.stream()
                .map(this::projectionToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert a ChatMessage entity to DTO.
     */
    public ChatMessageDto toDto(ChatMessage message) {
        return new ChatMessageDto(
                message.getId(),
                toDto(message.getSender()),
                message.getContent(),
                message.getCreatedAt(),
                message.getType().toString());
    }

    /**
     * Convert a ChatMessageProjection to DTO for better performance.
     */
    public ChatMessageDto projectionToDto(ChatMessageProjection projection) {
        UserDto senderDto = new UserDto(
                projection.getSender().getId(),
                projection.getSender().getName(),
                projection.getSender().getEmail());

        return new ChatMessageDto(
                projection.getId(),
                senderDto,
                projection.getContent(),
                projection.getCreatedAt(),
                projection.getType());
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail());
    }
}

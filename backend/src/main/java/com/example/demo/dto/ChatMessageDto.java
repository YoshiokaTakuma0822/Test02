package com.example.demo.dto;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;

import com.example.demo.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Data Transfer Object for ChatMessage.
 * Used for API responses to provide a clean and consistent interface.
 */
public record ChatMessageDto(
        @NonNull Long id,
        @NonNull UserDto sender,
        @NonNull String content,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Tokyo") @NonNull OffsetDateTime createdAt,
        @NonNull String type) {
    /**
     * Create DTO from entity.
     *
     * @param entity the chat message entity
     * @return a new DTO instance
     */
    public static @NonNull ChatMessageDto fromEntity(@NonNull ChatMessage entity) {
        return new ChatMessageDto(
                entity.getId(),
                UserDto.fromEntity(entity.getSender()),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getType().name());
    }
}

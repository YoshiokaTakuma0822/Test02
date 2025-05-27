package com.example.demo.projection;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Projection interface for ChatMessage to optimize query performance.
 * Only loads necessary fields, reducing memory usage and improving query speed.
 */
public interface ChatMessageProjection {
    Long getId();

    String getContent();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Tokyo")
    OffsetDateTime getCreatedAt();

    String getType();

    /**
     * Projection for User information to avoid N+1 query problem.
     */
    UserProjection getSender();

    interface UserProjection {
        Long getId();

        String getName();

        String getEmail();
    }
}

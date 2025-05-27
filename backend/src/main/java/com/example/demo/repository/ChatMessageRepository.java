package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ChatMessage;
import com.example.demo.projection.ChatMessageProjection;

import jakarta.annotation.Nonnull;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    /**
     * Get recent messages using projection to optimize query performance.
     * This avoids loading unnecessary entity data and reduces memory usage.
     */
    @Nonnull
    List<ChatMessageProjection> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Override
    @Nonnull
    <S extends ChatMessage> S save(@Nonnull S entity);

    @Override
    @Nonnull
    List<ChatMessage> findAll();

    /**
     * Fetch messages older than the given ID using projection, ordered
     * newest-first, limited by
     * pageable.
     */
    @Nonnull
    List<ChatMessageProjection> findByIdLessThanOrderByCreatedAtDescIdDesc(Long beforeId, Pageable pageable);
}

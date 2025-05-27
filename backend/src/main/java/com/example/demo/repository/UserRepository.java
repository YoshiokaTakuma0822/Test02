package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

import jakarta.annotation.Nonnull;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Override
    @Nonnull
    <S extends User> S save(@Nonnull S entity);

    @Override
    @Nonnull
    List<User> findAll();

    @Override
    @Nonnull
    Optional<User> findById(@Nullable Long id);

    @Nonnull
    Optional<User> findByEmail(@Nonnull String email);
}

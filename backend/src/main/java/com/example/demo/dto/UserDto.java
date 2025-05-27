package com.example.demo.dto;

import com.example.demo.entity.User;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for User.
 * Used for API responses to provide a clean and consistent interface.
 */
public record UserDto(
        Long id,
        @NotBlank(message = "Name must not be blank") String name,
        @NotBlank(message = "Email must not be blank") String email) {
    /**
     * Create a UserDto from a User entity.
     *
     * @param user the user entity
     * @return a new UserDto instance
     */
    public static UserDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }

    /**
     * Convert this DTO to a User entity.
     *
     * @return a new User entity
     */
    public User toEntity() {
        User entity = new User();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setEmail(this.email);
        return entity;
    }
}

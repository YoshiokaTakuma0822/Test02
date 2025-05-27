package com.example.demo.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.service.ActiveUsersService;
import com.example.demo.service.UserService;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;

/**
 * REST controller for user-related endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ActiveUsersService activeUsersService;

    /**
     * Get all users.
     *
     * @return list of all users
     */
    @GetMapping
    public @Nonnull ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID.
     *
     * @param id the user ID
     * @return the user if found, otherwise 404
     */
    @GetMapping("/{id}")
    public @Nonnull ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        Optional<User> userOptional = userService.getUserById(id);
        return userOptional
                .map(user -> ResponseEntity.ok(UserDto.fromEntity(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new user.
     *
     * @param userDto the user data
     * @return the created user
     */
    @PostMapping
    public @Nonnull ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        User savedUser = userService.saveUser(userDto.toEntity());
        activeUsersService.addUser(savedUser);
        return ResponseEntity.ok(UserDto.fromEntity(savedUser));
    }

    /**
     * Add a default user.
     *
     * @return the created default user
     */
    @GetMapping("/add-default")
    public @Nonnull ResponseEntity<UserDto> addDefaultUser() {
        User defaultUser = userService.createDefaultUser();
        activeUsersService.addUser(defaultUser);
        return ResponseEntity.ok(UserDto.fromEntity(defaultUser));
    }

    /**
     * Ensure default system users exist.
     *
     * @return list of all users after ensuring defaults
     */
    @GetMapping("/ensure-default-users")
    public @Nonnull ResponseEntity<List<UserDto>> ensureDefaultUsers() {
        List<UserDto> users = userService.ensureDefaultUsers().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * Get active users.
     *
     * @return list of active users
     */
    @GetMapping("/active")
    public @Nonnull ResponseEntity<List<UserDto>> getActiveUsers() {
        List<UserDto> users = activeUsersService.getActiveUsers();
        return ResponseEntity.ok(users);
    }
}

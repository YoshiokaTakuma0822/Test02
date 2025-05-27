package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ActiveUsersService;

@SpringBootTest
@Sql(scripts = "/test-data.sql")
class ActiveUsersIntegrationTest {

    @Autowired
    private ActiveUsersService activeUsersService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // clear Redis set before each test
        redisTemplate.delete("activeUsers");
    }

    @Test
    void shouldAddAndRetrieveActiveUsers() {
        // Given existing users loaded via test-data.sql
        User user1 = userRepository.findById(1L).orElseThrow();
        User user2 = userRepository.findById(2L).orElseThrow();

        // When
        activeUsersService.addUser(user1);
        activeUsersService.addUser(user2);

        // Then
        List<UserDto> activeUsers = activeUsersService.getActiveUsers();
        assertThat(activeUsers)
                .hasSize(2)
                .extracting(UserDto::id)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void shouldRemoveActiveUser() {
        // Given
        User user = userRepository.findById(1L).orElseThrow();
        activeUsersService.addUser(user);

        // When
        activeUsersService.removeUser(user);

        // Then
        List<UserDto> activeUsers = activeUsersService.getActiveUsers();
        assertThat(activeUsers).isEmpty();
    }
}

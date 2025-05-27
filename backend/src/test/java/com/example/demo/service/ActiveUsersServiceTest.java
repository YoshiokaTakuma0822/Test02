package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@SpringBootTest
class ActiveUsersServiceTest {

    @Autowired
    private ActiveUsersService activeUsersService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        // clear Redis set and database
        redisTemplate.delete("activeUsers");
    }

    @Test
    void shouldAddAndRetrieveActiveUser() {
        // create and save user
        User user = new User("Test User", "test@example.com");
        user = userRepository.save(user);

        // add to active users
        activeUsersService.addUser(user);

        // fetch active users
        List<UserDto> activeUsers = activeUsersService.getActiveUsers();
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).id()).isEqualTo(user.getId());
        assertThat(activeUsers.get(0).name()).isEqualTo("Test User");
        assertThat(activeUsers.get(0).email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldRemoveActiveUser() {
        // create and save user, then add
        User user = new User("Test User", "test@example.com");
        user = userRepository.save(user);
        activeUsersService.addUser(user);

        // remove from active users
        activeUsersService.removeUser(user);

        // verify none remain
        List<UserDto> activeUsers = activeUsersService.getActiveUsers();
        assertThat(activeUsers).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveUsers() {
        // no active users added
        List<UserDto> activeUsers = activeUsersService.getActiveUsers();
        assertThat(activeUsers).isEmpty();
    }
}

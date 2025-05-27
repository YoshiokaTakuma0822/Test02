package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ActiveUsersService {
    private static final String KEY = "activeUsers";
    private static final String USER_KEY_PREFIX = "activeUsers:user:";
    private static final String CHANNEL = "room:events";
    private static final long USER_TIMEOUT_SECONDS = 300; // 5分でタイムアウト（ハートビートなしの場合の推奨値）

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public void addUser(User user) {
        if (user != null && user.getId() != null) {
            String userId = user.getId().toString();
            String userKey = USER_KEY_PREFIX + userId;
            redisTemplate.opsForSet().add(KEY, userId);
            redisTemplate.opsForValue().set(userKey, "1", USER_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            try {
                // publish JSON notification
                String json = mapper.writeValueAsString(Map.of("type", "activeUsersUpdated"));
                redisTemplate.convertAndSend(CHANNEL, json);
            } catch (Exception e) {
                // JSON serialization of such a simple object should never fail
            }
        }
    }

    public void removeUser(User user) {
        if (user != null && user.getId() != null) {
            String userId = user.getId().toString();
            String userKey = USER_KEY_PREFIX + userId;
            redisTemplate.opsForSet().remove(KEY, userId);
            redisTemplate.delete(userKey);
            try {
                String json = mapper.writeValueAsString(Map.of("type", "activeUsersUpdated"));
                redisTemplate.convertAndSend(CHANNEL, json);
            } catch (Exception e) {
                // JSON serialization of such a simple object should never fail
            }
        }
    }

    public void updateUserTimeout(Long userId) {
        if (userId != null) {
            String userKey = USER_KEY_PREFIX + userId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
                redisTemplate.expire(userKey, USER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    public List<UserDto> getActiveUsers() {
        Set<String> ids = redisTemplate.opsForSet().members(KEY);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        // タイムアウトしたユーザーを削除
        ids.removeIf(id -> !Boolean.TRUE.equals(redisTemplate.hasKey(USER_KEY_PREFIX + id)));

        List<Long> userIds = ids.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        return userRepository.findAllById(userIds).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }
}

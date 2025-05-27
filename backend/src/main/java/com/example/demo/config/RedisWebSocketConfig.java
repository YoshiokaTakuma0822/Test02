package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.example.demo.service.WebSocketService;

/**
 * Redis configuration for WebSocket notifications.
 * Sets up message listener for Redis events to broadcast WebSocket
 * notifications.
 */
@Configuration
public class RedisWebSocketConfig {
    private static final String CHANNEL = "room:events";

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private WebSocketService webSocketService;

    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(webSocketService, new ChannelTopic(CHANNEL));
        return container;
    }
}

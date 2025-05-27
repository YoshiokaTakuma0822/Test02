package com.example.demo.service;

import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.example.demo.entity.ChatMessage;
import com.example.demo.repository.UserRepository;

/**
 * Centralized service for WebSocket functionality.
 * Handles session management, message broadcasting, and Redis message
 * listening.
 */
@Service
public class WebSocketService implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    private static final String WS_MESSAGE_TOPIC = "/topic/chat.messages.update";
    private static final String WS_USERS_TOPIC = "/topic/chat.users.update";

    // thread-safe map for sessionId to userId mapping
    private final ConcurrentHashMap<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActiveUsersService activeUsersService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatService chatService;

    /**
     * Handles user session connection.
     */
    public void handleUserConnect(String sessionId, Long userId) {
        sessionUserMap.put(sessionId, userId);
        userRepository.findById(userId).ifPresentOrElse(user -> {
            activeUsersService.addUser(user);
            notifyUsersUpdate();
            logger.info("Added user {} to active list", userId);

            ChatMessage joinMessage = messageService.createJoinMessage(user);
            chatService.createMessageFromEntity(joinMessage);
            broadcastMessage();
            logger.debug("Created join message for user {}", user.getName());
        }, () -> {
            logger.warn("User not found for ID: {}", userId);
        });
    }

    /**
     * Handles user session disconnect.
     */
    public void handleUserDisconnect(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            logger.info("User disconnected: {}", userId);

            userRepository.findById(userId).ifPresentOrElse(user -> {
                ChatMessage leaveMessage = messageService.createLeaveMessage(user);
                chatService.createMessageFromEntity(leaveMessage);
                broadcastMessage();
                logger.debug("Created leave message for user {}", user.getName());

                activeUsersService.removeUser(user);
                notifyUsersUpdate();
            }, () -> {
                logger.warn("User not found for ID: {}", userId);
            });
        }
    }

    /**
     * Updates user activity timeout on subscription.
     */
    public void handleUserSubscribe(String sessionId, String destination) {
        Long userId = sessionUserMap.get(sessionId);
        if (userId != null) {
            logger.debug("User {} subscribed to {}", userId, destination);
            activeUsersService.updateUserTimeout(userId);
        }
    }

    /**
     * Gets the user ID associated with a session ID.
     */
    public Long getUserIdBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    /**
     * Sends an update signal to all clients that new messages are available.
     * Clients should fetch the latest messages via REST API upon receiving this
     * notification.
     * The payload is intentionally empty as clients only use this as a trigger.
     */
    public void broadcastMessage() {
        messagingTemplate.convertAndSend(WS_MESSAGE_TOPIC, "");
        logger.debug("Broadcasted message update notification");
    }

    /**
     * Sends an update signal to all clients that the user list has changed.
     * Clients should fetch the latest active users via REST API upon receiving this
     * notification.
     * The payload is intentionally empty as clients only use this as a trigger.
     */
    public void notifyUsersUpdate() {
        messagingTemplate.convertAndSend(WS_USERS_TOPIC, "");
        logger.debug("Broadcasted users update notification");
    }

    /**
     * Handles Redis message events.
     *
     * @param message Redis message containing the payload and channel information
     * @param pattern Pattern that the channel matches
     */
    @Override
    public void onMessage(@NonNull Message message, byte @NonNull [] pattern) {
        try {
            broadcastMessage();
        } catch (Exception e) {
            logger.error("Error processing Redis message", e);
        }
    }
}

package com.example.demo.websocket;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.example.demo.service.WebSocketService;

/**
 * Listener for WebSocket events.
 * This component listens for various WebSocket lifecycle events and delegates
 * their handling to the WebSocketService. It acts as a thin wrapper
 * around Spring's WebSocket events, extracting relevant information and
 * passing it to the appropriate service methods.
 */
@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private WebSocketService webSocketService;

    /**
     * Handles initial WebSocket connection attempts.
     * Extracts the userId from headers and initializes the session.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        Message<?> message = event.getMessage();
        if (message != null) {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
            String userIdStr = headerAccessor.getFirstNativeHeader("userId");
            String sessionId = headerAccessor.getSessionId();
            logger.debug("New WebSocket connection attempt. SessionId: {}, UserId: {}", sessionId, userIdStr);

            if (userIdStr != null && sessionId != null) {
                try {
                    Long userId = Long.valueOf(userIdStr);
                    // Store userId in session attributes if available
                    Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
                    if (sessionAttrs != null) {
                        sessionAttrs.put("userId", userId);
                    }
                    // Handle user connection regardless of session attributes
                    webSocketService.handleUserConnect(sessionId, userId);
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid userId header: {}", userIdStr);
                }
            }
        }
    }

    /**
     * Handles successful WebSocket connections.
     * Updates session mapping when connection is established.
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Message<?> message = event.getMessage();
        if (message != null) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            String sessionId = accessor.getSessionId();
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            logger.debug("WebSocket connection established. SessionId: {}", sessionId);

            if (sessionId != null && sessionAttrs != null && sessionAttrs.get("userId") != null) {
                try {
                    Long userId = Long.valueOf(sessionAttrs.get("userId").toString());
                    webSocketService.handleUserConnect(sessionId, userId);
                    logger.debug("User connection established. SessionId: {}, UserId: {}", sessionId, userId);
                } catch (NumberFormatException ex) {
                    logger.warn("Invalid userId in session attributes: {}", sessionAttrs.get("userId"));
                }
            }
        }
    }

    /**
     * Handles WebSocket disconnection events.
     * Cleans up session state and notifies other users.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            webSocketService.handleUserDisconnect(sessionId);
            logger.debug("User disconnected. SessionId: {}", sessionId);
        }
    }

    /**
     * Processes subscription requests to specific topics.
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        webSocketService.handleUserSubscribe(
                headerAccessor.getSessionId(),
                headerAccessor.getDestination());
    }

    /**
     * Processes unsubscription requests from topics.
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        webSocketService.handleUserSubscribe(
                headerAccessor.getSessionId(),
                headerAccessor.getDestination());
    }
}

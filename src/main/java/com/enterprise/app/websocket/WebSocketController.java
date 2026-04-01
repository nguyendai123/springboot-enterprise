package com.enterprise.app.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // Track connected sessions
    private final Set<String> connectedSessions = ConcurrentHashMap.newKeySet();

    // ── STOMP events ──────────────────────────────────────────────

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        connectedSessions.add(sessionId);
        log.info("WebSocket connected: sessionId={}, total={}", sessionId, connectedSessions.size());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        connectedSessions.remove(sessionId);
        log.info("WebSocket disconnected: sessionId={}, total={}", sessionId, connectedSessions.size());
    }

    // ── Message handlers ──────────────────────────────────────────

    /** Client sends to /app/chat.send → broadcast to /topic/public */
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Chat message from user={}: {}", msg.sender(), msg.content());
        return new ChatMessage(msg.sender(), msg.content(), LocalDateTime.now().toString(), "CHAT");
    }

    /** Client sends to /app/chat.addUser → register username in session */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", msg.sender());
        log.info("User joined chat: {}", msg.sender());
        return new ChatMessage("SERVER", msg.sender() + " joined!", LocalDateTime.now().toString(), "JOIN");
    }

    /** Client sends to /app/private → delivered only to specific user via /user/queue/private */
    @MessageMapping("/private")
    @SendToUser("/queue/private")
    public ChatMessage privateMessage(@Payload ChatMessage msg, java.security.Principal principal) {
        log.info("Private message from {} to user queue", principal.getName());
        return msg;
    }

    // ── Server-push helpers ───────────────────────────────────────

    /** Push notification to a specific user */
    public void notifyUser(String userId, Object notification) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
        log.debug("Pushed notification to userId={}", userId);
    }

    /** Broadcast to all subscribers of /topic/updates */
    public void broadcast(String topic, Object payload) {
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
    }

    /** Heartbeat every 30s to keep connections alive and push stats */
    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        if (!connectedSessions.isEmpty()) {
            broadcast("heartbeat", Map.of(
                    "time",       LocalDateTime.now().toString(),
                    "connected",  connectedSessions.size()
            ));
        }
    }

    // ── DTOs ─────────────────────────────────────────────────────
    public record ChatMessage(String sender, String content, String timestamp, String type) {}
}
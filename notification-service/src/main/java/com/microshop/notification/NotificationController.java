package com.microshop.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** GET /api/notifications : 30 thong bao moi nhat cua user dang dang nhap. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    public record NotificationResponse(UUID id, UUID orderId, String type, String title, String message, Instant createdAt) {
    }

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return notifications.findTop30ByUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getOrderId(), n.getType(), n.getTitle(), n.getMessage(), n.getCreatedAt()))
                .toList();
    }
}

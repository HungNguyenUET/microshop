package com.microshop.notification;

import com.microshop.notification.events.OrderEvent;
import com.microshop.notification.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Consumer group "notification-service" doc CUNG topic order-events voi payment-service,
 * nhung moi group nhan 1 ban copy rieng (pub/sub). order-service khong he biet notification-service ton tai:
 * them/bot consumer khong phai sua producer -> loose coupling.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private static final NumberFormat VND = NumberFormat.getInstance(Locale.of("vi", "VN"));

    private final NotificationRepository notifications;
    private final JsonMapper json;

    public OrderEventListener(NotificationRepository notifications, JsonMapper json) {
        this.notifications = notifications;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS)
    @Transactional
    public void onOrderEvent(String message) {
        OrderEvent event = json.readValue(message, OrderEvent.class);
        if (notifications.existsByEventId(event.eventId())) {
            return;   // event giao lai -> bo qua
        }
        switch (event) {
            case OrderEvent.OrderPlaced e -> save(e, "ORDER_PLACED", "Đặt hàng thành công",
                    "Đơn " + shortId(e) + " gồm " + e.itemCount() + " sản phẩm, tổng " + VND.format(e.totalAmount()) + " đ đang chờ thanh toán.");
            case OrderEvent.OrderPaid e -> save(e, "ORDER_PAID", "Thanh toán thành công",
                    "Đơn " + shortId(e) + " đã thanh toán " + VND.format(e.amount()) + " đ. Cảm ơn " + e.username() + "!");
            case OrderEvent.OrderPaymentFailed e -> save(e, "PAYMENT_FAILED", "Thanh toán thất bại",
                    "Đơn " + shortId(e) + ": " + e.reason() + ". Hãy nạp thêm tiền và thử lại.");
            case OrderEvent.PaymentRequested _ -> {
                // event noi bo giua order va payment, khong bao cho user
            }
        }
    }

    private void save(OrderEvent event, String type, String title, String message) {
        notifications.save(new Notification(event.userId(), event.orderId(), type, title, message, event.eventId()));
        // Thuc te: gui email (SMTP/SES), push notification, SMS... O day chi ghi log.
        log.info("[NOTIFY user={}] {} - {}", event.userId(), title, message);
    }

    private static String shortId(OrderEvent event) {
        return "#" + event.orderId().toString().substring(0, 8);
    }
}

package com.microshop.order.cart;

import com.microshop.order.OrderExceptions;
import com.microshop.order.client.ProductClient;
import com.microshop.order.client.ProductClient.ProductInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    public record CartLine(UUID productId, String productName, BigDecimal unitPrice, int quantity,
                           BigDecimal lineTotal, boolean available) {
    }

    public record CartView(List<CartLine> items, BigDecimal total) {
    }

    private final CartItemRepository cart;
    private final ProductClient productClient;

    public CartService(CartItemRepository cart, ProductClient productClient) {
        this.cart = cart;
        this.productClient = productClient;
    }

    /**
     * Ghep du lieu tu 2 service: so luong (order_db) + ten/gia (goi REST sang product-service).
     * Day la cach "join" du lieu trong microservices - khong the JOIN 2 database bang SQL.
     */
    public CartView view(String userId) {
        List<CartItem> items = cart.findByUserIdOrderByAddedAt(userId);
        if (items.isEmpty()) {
            return new CartView(List.of(), BigDecimal.ZERO);
        }
        Map<UUID, ProductInfo> products = loadProducts(items);
        List<CartLine> lines = items.stream().map(item -> {
            ProductInfo p = products.get(item.getProductId());
            boolean available = p != null && p.active();
            BigDecimal price = available ? p.price() : BigDecimal.ZERO;
            return new CartLine(item.getProductId(), p != null ? p.name() : "(san pham khong ton tai)",
                    price, item.getQuantity(), price.multiply(BigDecimal.valueOf(item.getQuantity())), available);
        }).toList();
        BigDecimal total = lines.stream().map(CartLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(lines, total);
    }

    @Transactional
    public void add(String userId, UUID productId, int quantity) {
        ProductInfo product = productClient.findByIds(List.of(productId)).stream().findFirst()
                .filter(ProductInfo::active)
                .orElseThrow(() -> new OrderExceptions.InvalidRequest("San pham " + productId + " khong ton tai"));
        cart.findByUserIdAndProductId(userId, product.id())
                .ifPresentOrElse(item -> item.increase(quantity),
                        () -> cart.save(new CartItem(userId, product.id(), quantity)));
    }

    @Transactional
    public void update(String userId, UUID productId, int quantity) {
        CartItem item = cart.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new OrderExceptions.NotFound("San pham khong co trong gio"));
        item.setQuantity(quantity);
    }

    @Transactional
    public void remove(String userId, UUID productId) {
        cart.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void clear(String userId) {
        cart.deleteByUserId(userId);
    }

    public List<CartItem> rawItems(String userId) {
        return cart.findByUserIdOrderByAddedAt(userId);
    }

    public Map<UUID, ProductInfo> loadProducts(List<CartItem> items) {
        return productClient.findByIds(items.stream().map(CartItem::getProductId).toList()).stream()
                .collect(Collectors.toMap(ProductInfo::id, Function.identity()));
    }
}

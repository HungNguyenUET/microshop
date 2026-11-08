package com.microshop.order.cart;

import com.microshop.order.cart.CartService.CartView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gio hang cua user dang dang nhap (user lay tu JWT, KHONG lay tu URL -> khong sua duoc gio cua nguoi khac).
 * <pre>
 * GET    /api/cart                       xem gio
 * POST   /api/cart/items                 them {productId, quantity}
 * PUT    /api/cart/items/{productId}     doi so luong {quantity}
 * DELETE /api/cart/items/{productId}     bo 1 san pham
 * DELETE /api/cart                       xoa het
 * </pre>
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    public record AddItemRequest(@NotNull UUID productId, @Min(1) @Max(99) int quantity) {
    }

    public record UpdateQuantityRequest(@Min(1) @Max(99) int quantity) {
    }

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartView view(@AuthenticationPrincipal Jwt jwt) {
        return cart.view(jwt.getSubject());
    }

    @PostMapping("/items")
    public CartView add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest request) {
        cart.add(jwt.getSubject(), request.productId(), request.quantity());
        return cart.view(jwt.getSubject());
    }

    @PutMapping("/items/{productId}")
    public CartView update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                           @Valid @RequestBody UpdateQuantityRequest request) {
        cart.update(jwt.getSubject(), productId, request.quantity());
        return cart.view(jwt.getSubject());
    }

    @DeleteMapping("/items/{productId}")
    public CartView remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        cart.remove(jwt.getSubject(), productId);
        return cart.view(jwt.getSubject());
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt) {
        cart.clear(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}

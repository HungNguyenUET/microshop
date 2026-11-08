package com.microshop.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Cau hinh rieng cua auth-service (prefix "microshop.auth" trong application.yml).
 *
 * @param issuer            URL ma browser nhin thay; chinh la claim "iss" trong moi token.
 * @param adminGithubLogins GitHub username tu dong duoc cap role ADMIN o lan dang nhap dau tien.
 * @param gateway           client "api-gateway" (Authorization Code + PKCE + Refresh Token).
 * @param orderService      client "order-service" (Client Credentials - goi service khac).
 * @param demoUsers         tao san tai khoan demo (user/user, admin/admin) de hoc khi chua co GitHub OAuth App.
 */
@ConfigurationProperties(prefix = "microshop.auth")
public record AuthProperties(
        String issuer,
        List<String> adminGithubLogins,
        GatewayClient gateway,
        ServiceClient orderService,
        boolean demoUsers) {

    public record GatewayClient(String clientId, String clientSecret, String redirectUri, String postLogoutRedirectUri) {
    }

    public record ServiceClient(String clientId, String clientSecret) {
    }
}

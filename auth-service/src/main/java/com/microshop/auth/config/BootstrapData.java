package com.microshop.auth.config;

import com.microshop.auth.user.AppUser;
import com.microshop.auth.user.AppUserRepository;
import com.microshop.auth.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Khoi tao du lieu lan dau chay:
 * <ul>
 *   <li>Dang ky 2 OAuth2 client: api-gateway va order-service.</li>
 *   <li>Tao tai khoan demo (neu microshop.auth.demo-users=true).</li>
 * </ul>
 */
@Component
public class BootstrapData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapData.class);

    private final RegisteredClientRepository clients;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;

    public BootstrapData(RegisteredClientRepository clients, AppUserRepository users,
                         PasswordEncoder passwordEncoder, AuthProperties props) {
        this.clients = clients;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        registerGatewayClient();
        registerOrderServiceClient();
        if (props.demoUsers()) {
            createDemoUser("user", "Demo User", EnumSet.of(Role.USER));
            createDemoUser("admin", "Demo Admin", EnumSet.of(Role.USER, Role.ADMIN));
        }
    }

    /**
     * api-gateway = "Backend For Frontend". Client CONFIDENTIAL (co secret, luu o server, khong lo ra browser).
     * Authorization Code + PKCE + Refresh Token: dung luong khuyen nghi cua OAuth 2.1.
     */
    private void registerGatewayClient() {
        var cfg = props.gateway();
        if (clients.findByClientId(cfg.clientId()) != null) {
            return;
        }
        clients.save(RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(cfg.clientId())
                .clientName("MicroShop API Gateway")
                .clientSecret(passwordEncoder.encode(cfg.clientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(cfg.redirectUri())
                .postLogoutRedirectUri(cfg.postLogoutRedirectUri())
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)                 // bat buoc PKCE
                        .requireAuthorizationConsent(false)    // first-party app, khong hien man hinh consent
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))   // ngan: lo token thi thiet hai nho
                        .refreshTokenTimeToLive(Duration.ofHours(8))
                        .reuseRefreshTokens(false)                      // refresh token rotation
                        .build())
                .build());
        log.info("Registered OAuth2 client '{}'", cfg.clientId());
    }

    /** order-service goi product-service: dung token cua CHINH order-service (khong co user). */
    private void registerOrderServiceClient() {
        var cfg = props.orderService();
        if (clients.findByClientId(cfg.clientId()) != null) {
            return;
        }
        clients.save(RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(cfg.clientId())
                .clientName("Order Service (service-to-service)")
                .clientSecret(passwordEncoder.encode(cfg.clientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("product.read")
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofMinutes(5)).build())
                .build());
        log.info("Registered OAuth2 client '{}'", cfg.clientId());
    }

    private void createDemoUser(String username, String displayName, EnumSet<Role> roles) {
        if (users.existsByUsername(username)) {
            return;
        }
        users.save(AppUser.local(username, displayName, passwordEncoder.encode(username), roles));
        log.info("Created demo user '{}' (password = username) with roles {}", username, roles);
    }
}

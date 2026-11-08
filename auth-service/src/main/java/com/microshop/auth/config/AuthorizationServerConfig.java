package com.microshop.auth.config;

import com.microshop.auth.user.AppUser;
import com.microshop.auth.user.AppUserRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Phan "Authorization Server" (OAuth 2.1 + OpenID Connect 1.0).
 * <p>
 * Endpoint chinh (xem http://localhost:9000/.well-known/openid-configuration):
 * <pre>
 * /oauth2/authorize   - browser duoc redirect toi day de dang nhap
 * /oauth2/token       - doi code lay token, refresh token, client_credentials
 * /oauth2/jwks        - public key de cac service tu verify chu ky JWT (khong can goi lai auth-service)
 * /userinfo           - thong tin user (OIDC)
 * /connect/logout     - OIDC RP-initiated logout
 * </pre>
 */
@Configuration
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .oauth2AuthorizationServer(as -> {
                    // Chain nay CHI ap dung cho cac endpoint cua Authorization Server
                    http.securityMatcher(as.getEndpointsMatcher());
                    as.oidc(Customizer.withDefaults());
                })
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                // Chua dang nhap ma vao /oauth2/authorize tu browser -> chuyen sang trang /login
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                // /userinfo nhan Bearer access token
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // ------------------------------------------------------------------ luu tru (PostgreSQL)

    /** Client (gateway, order-service) luu trong bang oauth2_registered_client, xem ClientRegistrar. */
    @Bean
    RegisteredClientRepository registeredClientRepository(JdbcOperations jdbc) {
        return new JdbcRegisteredClientRepository(jdbc);
    }

    /** Code / access token / refresh token da cap: restart auth-service van khong mat phien. */
    @Bean
    OAuth2AuthorizationService authorizationService(JdbcOperations jdbc, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationService(jdbc, clients);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(JdbcOperations jdbc, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
    }

    // ------------------------------------------------------------------ ky / verify JWT

    @Bean
    JWKSource<SecurityContext> jwkSource(SigningKeyStore keyStore) {
        JWKSet jwkSet = new JWKSet(keyStore.loadOrCreate());
        return (selector, context) -> selector.select(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(AuthProperties props) {
        // Co dinh issuer: token cap cho browser (localhost:9000) va cho container (auth-service:9000)
        // deu co cung "iss" -> resource server validate on dinh.
        return AuthorizationServerSettings.builder().issuer(props.issuer()).build();
    }

    // ------------------------------------------------------------------ noi dung token

    /**
     * Them claim vao access token va ID token:
     * <pre>
     * {
     *   "sub": "8b1c...-uuid",            // id noi bo cua user (khong phai GitHub id)
     *   "preferred_username": "octocat",
     *   "name": "The Octocat",
     *   "picture": "https://avatars.githubusercontent.com/...",
     *   "roles": ["USER", "ADMIN"]       // cac service doc claim nay de phan quyen
     * }
     * </pre>
     * Role duoc doc lai tu DB moi lan cap token -> admin doi role, user nhan quyen moi o lan refresh token ke tiep.
     */
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(AppUserRepository users) {
        return context -> {
            boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
            boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
            if (!accessToken && !idToken) {
                return;
            }

            // Service goi service (client_credentials): khong co user, chi co role SERVICE
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                context.getClaims().claim("roles", new ArrayList<>(List.of("SERVICE")));
                return;
            }

            resolveUser(context.getPrincipal(), users).ifPresent(user -> context.getClaims().claims(claims -> {
                claims.put("sub", user.getId().toString());
                claims.put("preferred_username", user.getUsername());
                // ArrayList (khong dung List.of / toList): claim duoc luu vao bang oauth2_authorization bang Jackson,
                // ma Jackson chi cho phep deserialize cac kieu collection "quen thuoc" (allowlist bao mat).
                claims.put("roles", user.getRoles().stream().map(Enum::name).sorted()
                        .collect(Collectors.toCollection(ArrayList::new)));
                putIfNotNull(claims, "name", user.getDisplayName());
                putIfNotNull(claims, "email", user.getEmail());
                putIfNotNull(claims, "picture", user.getAvatarUrl());
            }));
        };
    }

    /** GitHub login: principal name = user id (xem GithubOAuth2UserService). Form login: principal name = username. */
    private static Optional<AppUser> resolveUser(Authentication principal, AppUserRepository users) {
        if (principal.getPrincipal() instanceof OAuth2User) {
            return users.findById(UUID.fromString(principal.getName()));
        }
        return users.findByUsername(principal.getName());
    }

    private static void putIfNotNull(java.util.Map<String, Object> claims, String key, Object value) {
        if (value != null) {
            claims.put(key, value);
        }
    }
}

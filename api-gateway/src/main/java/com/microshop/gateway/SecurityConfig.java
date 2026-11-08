package com.microshop.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * API Gateway theo mo hinh BFF (Backend For Frontend):
 * <ul>
 *   <li>Browser KHONG BAO GIO cam access token. Token nam trong session cua gateway (luu o Redis).
 *       Browser chi co cookie session HttpOnly -> JavaScript doc khong duoc -> chong XSS lay token.</li>
 *   <li>Gateway gan token vao header Authorization khi forward xuong service (TokenRelay).</li>
 *   <li>Vi dung cookie nen PHAI bat CSRF.</li>
 *   <li>Postman/curl co the goi thang bang Bearer token (gateway dong vai tro resource server).</li>
 * </ul>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    static final String REGISTRATION_ID = "microshop";

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  ServerOAuth2AuthorizationRequestResolver pkceResolver,
                                                  OidcLogoutHandler logoutHandler) {
        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/", "/index.html", "/app.js", "/app.css", "/favicon.ico").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/prometheus", "/fallback/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // Defense in depth: chan som o gateway, service phia sau van tu kiem tra lai
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2Login(login -> login
                        .authorizationRequestResolver(pkceResolver)
                        // Login loi -> ve trang chu kem ?loginError (tranh vong lap redirect vo han)
                        .authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/?loginError")))
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(bearerTokenConverter())))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint()))
                .csrf(csrf -> csrf
                        // Cookie XSRF-TOKEN (khong HttpOnly) -> JS doc va gui lai qua header X-XSRF-TOKEN
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(csrfRequiredMatcher()))
                .logout(logout -> logout.logoutSuccessHandler(logoutHandler))
                .build();
    }

    /** API tra 401 (de JS xu ly), trang HTML thi redirect thang sang auth-service de dang nhap. */
    private static DelegatingServerAuthenticationEntryPoint entryPoint() {
        var api = new DelegatingServerAuthenticationEntryPoint.DelegateEntry(
                new PathPatternParserServerWebExchangeMatcher("/api/**"),
                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED));
        var entryPoint = new DelegatingServerAuthenticationEntryPoint(api);
        entryPoint.setDefaultEntryPoint(
                new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/" + REGISTRATION_ID));
        return entryPoint;
    }

    /** Chi request bang cookie session moi can CSRF. Request mang Bearer token thi khong (browser khong tu gui header nay). */
    private static ServerWebExchangeMatcher csrfRequiredMatcher() {
        Set<HttpMethod> safe = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);
        return exchange -> {
            var request = exchange.getRequest();
            String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            boolean bearer = auth != null && auth.startsWith("Bearer ");
            return safe.contains(request.getMethod()) || bearer
                    ? ServerWebExchangeMatcher.MatchResult.notMatch()
                    : ServerWebExchangeMatcher.MatchResult.match();
        };
    }

    /** WebFlux sinh CSRF token "lazy": phai subscribe thi cookie XSRF-TOKEN moi duoc ghi ra. */
    @Bean
    WebFilter csrfCookieWebFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
            return (token != null ? token.then() : Mono.<Void>empty()).then(chain.filter(exchange));
        };
    }

    /** PKCE cho ca confidential client (OAuth 2.1 khuyen nghi), auth-service cung bat buoc requireProofKey. */
    @Bean
    ServerOAuth2AuthorizationRequestResolver pkceResolver(ReactiveClientRegistrationRepository registrations) {
        var resolver = new DefaultServerOAuth2AuthorizationRequestResolver(registrations);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /**
     * Login qua browser: lay claim "roles" trong ID token (do auth-service cap) -> ROLE_USER, ROLE_ADMIN.
     * Nho vay gateway phan quyen duoc (vd: /api/admin/** chi ADMIN).
     */
    @Bean
    ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        var delegate = new OidcReactiveOAuth2UserService();
        return request -> delegate.loadUser(request).map(user -> {
            Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
            List<String> roles = user.getIdToken().getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            return new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo(), "sub");
        });
    }

    /** Goi bang Bearer token (Postman): doc claim "roles" trong access token. */
    private static Converter<Jwt, Mono<AbstractAuthenticationToken>> bearerTokenConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    @Bean
    OidcLogoutHandler oidcLogoutHandler(@Value("${microshop.auth.public-url}") String authPublicUrl,
                                        @Value("${microshop.gateway.public-url}") String gatewayPublicUrl) {
        return new OidcLogoutHandler(URI.create(authPublicUrl + "/connect/logout"), gatewayPublicUrl + "/");
    }
}

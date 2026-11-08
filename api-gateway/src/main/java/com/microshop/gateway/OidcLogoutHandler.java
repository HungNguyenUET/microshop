package com.microshop.gateway;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Single Logout (OIDC RP-Initiated Logout):
 * xoa session o gateway XONG thi redirect browser sang auth-service /connect/logout de xoa ca session dang nhap o do.
 * Neu khong, bam "Dang nhap" lan nua se tu dong vao luon ma khong hoi mat khau.
 */
public class OidcLogoutHandler implements ServerLogoutSuccessHandler {

    private final URI endSessionEndpoint;
    private final String postLogoutRedirectUri;
    private final DefaultServerRedirectStrategy redirect = new DefaultServerRedirectStrategy();

    public OidcLogoutHandler(URI endSessionEndpoint, String postLogoutRedirectUri) {
        this.endSessionEndpoint = endSessionEndpoint;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        URI target = URI.create(postLogoutRedirectUri);
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            target = UriComponentsBuilder.fromUri(endSessionEndpoint)
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue())
                    .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
                    .build().toUri();
        }
        return redirect.sendRedirect(exchange.getExchange(), target);
    }
}

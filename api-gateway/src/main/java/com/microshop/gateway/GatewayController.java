package com.microshop.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class GatewayController {

    /** UI goi de biet ai dang dang nhap. KHONG tra access token ra browser (BFF). */
    @GetMapping("/api/me")
    Map<String, Object> me(@AuthenticationPrincipal OidcUser user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .sorted().toList();
        return Map.of(
                "id", user.getSubject(),
                "username", user.getPreferredUsername(),
                "name", user.getFullName() != null ? user.getFullName() : user.getPreferredUsername(),
                "picture", user.getPicture() != null ? user.getPicture() : "",
                "roles", roles);
    }

    /** Circuit breaker mo / service cham -> tra loi ngay 503 thay vi de user cho. */
    @RequestMapping("/fallback/{service}")
    ResponseEntity<ProblemDetail> fallback(@PathVariable String service) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                service + " tạm thời không phản hồi, vui lòng thử lại sau.");
        problem.setTitle("Service Unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}

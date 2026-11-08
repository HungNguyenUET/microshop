package com.microshop.auth.api;

import com.microshop.auth.user.AppUser;
import com.microshop.auth.user.AppUserRepository;
import com.microshop.auth.user.Role;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <pre>
 * GET /api/users/me                    profile cua user dang dang nhap
 * GET /api/admin/users                 (ADMIN) danh sach user
 * PUT /api/admin/users/{id}/roles      (ADMIN) phan quyen, body: {"roles":["USER","ADMIN"]}
 * </pre>
 */
@RestController
public class UserController {

    public record UserResponse(UUID id, String username, String displayName, String email, String avatarUrl,
                               String provider, Set<Role> roles, Instant createdAt, Instant lastLoginAt) {
        static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getEmail(), u.getAvatarUrl(),
                    u.getProvider().name(), u.getRoles(), u.getCreatedAt(), u.getLastLoginAt());
        }
    }

    public record ChangeRolesRequest(@NotEmpty Set<Role> roles) {
    }

    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject())).map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/api/admin/users")
    public List<UserResponse> all() {
        return users.findAll().stream().map(UserResponse::from).toList();
    }

    @PutMapping("/api/admin/users/{id}/roles")
    @Transactional
    public UserResponse changeRoles(@PathVariable UUID id, @RequestBody @jakarta.validation.Valid ChangeRolesRequest request) {
        AppUser user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.changeRoles(request.roles());
        // Token hien tai cua user van giu role cu toi khi het han (5 phut) -> stateless JWT trade-off.
        return UserResponse.from(user);
    }
}

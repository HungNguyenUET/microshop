package com.microshop.auth.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * FEDERATED LOGIN: GitHub chi dung de XAC THUC danh tinh.
 * Sau khi GitHub tra ve profile, auth-service tao/cap nhat user trong DB cua minh,
 * gan role cua MicroShop va tu phat JWT rieng. Cac service phia sau KHONG bao gio thay token cua GitHub.
 */
@Service
public class GithubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuth2UserService.class);
    public static final String USER_ID_ATTRIBUTE = "microshop_user_id";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    private final AppUserRepository users;
    private final AdminPolicy adminPolicy;

    public GithubOAuth2UserService(AppUserRepository users, AdminPolicy adminPolicy) {
        this.users = users;
        this.adminPolicy = adminPolicy;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User github = delegate.loadUser(request);   // goi https://api.github.com/user
        String githubId = String.valueOf(github.getAttributes().get("id"));
        String login = (String) github.getAttributes().get("login");

        AppUser user = users.findByProviderAndProviderUserId(AuthProvider.GITHUB, githubId)
                .orElseGet(() -> {
                    var roles = adminPolicy.isAdminGithubLogin(login) ? EnumSet.of(Role.USER, Role.ADMIN) : EnumSet.of(Role.USER);
                    log.info("First GitHub login of '{}', create user with roles {}", login, roles);
                    return users.save(AppUser.github(uniqueUsername(login), githubId, roles));
                });
        user.updateProfile(
                (String) github.getAttributes().getOrDefault("name", login),
                (String) github.getAttributes().get("email"),
                (String) github.getAttributes().get("avatar_url"));
        user.markLogin();

        // Principal name = id noi bo cua user (xem tokenCustomizer).
        // Chi giu vai thuoc tinh, dung DefaultOAuth2User (kieu chuan) de serialize duoc vao oauth2_authorization.
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(USER_ID_ATTRIBUTE, user.getId().toString());
        attributes.put("login", login);
        return new DefaultOAuth2User(
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList(),
                attributes,
                USER_ID_ATTRIBUTE);
    }

    /** Tranh trung voi tai khoan LOCAL cung ten (vd: GitHub user ten "admin"). */
    private String uniqueUsername(String login) {
        return users.existsByUsername(login) ? login + "-github" : login;
    }
}

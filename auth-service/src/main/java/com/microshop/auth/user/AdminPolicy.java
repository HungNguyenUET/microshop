package com.microshop.auth.user;

import com.microshop.auth.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminPolicy {

    private final List<String> adminGithubLogins;

    public AdminPolicy(AuthProperties props) {
        this.adminGithubLogins = props.adminGithubLogins() == null ? List.of() : props.adminGithubLogins();
    }

    public boolean isAdminGithubLogin(String login) {
        return adminGithubLogins.stream().anyMatch(admin -> admin.equalsIgnoreCase(login));
    }
}

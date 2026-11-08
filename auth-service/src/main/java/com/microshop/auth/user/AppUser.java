package com.microshop.auth.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "display_name")
    private String displayName;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    protected AppUser() {
    }

    private AppUser(String username, AuthProvider provider, Set<Role> roles) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.provider = provider;
        this.roles = EnumSet.copyOf(roles);
        this.createdAt = Instant.now();
    }

    public static AppUser local(String username, String displayName, String passwordHash, Set<Role> roles) {
        AppUser user = new AppUser(username, AuthProvider.LOCAL, roles);
        user.displayName = displayName;
        user.passwordHash = passwordHash;
        return user;
    }

    public static AppUser github(String username, String githubId, Set<Role> roles) {
        AppUser user = new AppUser(username, AuthProvider.GITHUB, roles);
        user.providerUserId = githubId;
        return user;
    }

    /** Dong bo profile tu GitHub moi lan dang nhap. */
    public void updateProfile(String displayName, String email, String avatarUrl) {
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    public void markLogin() {
        this.lastLoginAt = Instant.now();
    }

    public void changeRoles(Set<Role> newRoles) {
        this.roles = EnumSet.copyOf(newRoles);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public AuthProvider getProvider() { return provider; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
}

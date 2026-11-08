package com.microshop.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    boolean existsByUsername(String username);
}

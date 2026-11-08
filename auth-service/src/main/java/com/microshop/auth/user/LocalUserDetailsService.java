package com.microshop.auth.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Dang nhap bang username/password (chi ap dung cho user LOCAL - tai khoan demo). */
@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public LocalUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .filter(u -> u.getProvider() == AuthProvider.LOCAL)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        // Tra ve org.springframework.security.core.userdetails.User (kieu chuan) vi principal nay
        // duoc serialize vao bang oauth2_authorization.
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(user.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList())
                .build();
    }
}

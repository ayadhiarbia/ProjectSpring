package com.mycompany.platforme_telemedcine.Models;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class CustomUserDetails extends User {
    private final UserStatus status;
    private final Long userId;

    // Only one constructor that includes userId
    public CustomUserDetails(String username, String password, boolean enabled,
                             boolean accountNonExpired, boolean credentialsNonExpired,
                             boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities,
                             UserStatus status, Long userId) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.status = status;
        this.userId = userId;
    }

    // Getters
    public Long getUserId() {
        return userId;
    }

    public UserStatus getStatus() {
        return status;
    }
}
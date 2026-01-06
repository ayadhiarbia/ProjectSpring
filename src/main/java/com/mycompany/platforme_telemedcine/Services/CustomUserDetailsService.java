package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.CustomUserDetails;
import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        return createCustomUserDetails(user);
    }

    private CustomUserDetails createCustomUserDetails(User user) {
        // Build authorities
        String roleWithPrefix = "ROLE_" + user.getRole().name();
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));

        // Determine if account is enabled based on status
        boolean enabled = user.getStatus() == UserStatus.APPROVED;

        // ALWAYS pass the userId - this is crucial!
        return new CustomUserDetails(
                user.getEmail(),           // username
                user.getPassword(),        // password
                enabled,                   // enabled
                true,                      // accountNonExpired
                true,                      // credentialsNonExpired
                true,                      // accountNonLocked
                authorities,               // authorities
                user.getStatus(),          // status
                user.getId()               // userId - MAKE SURE THIS IS NOT NULL!
        );
    }
}
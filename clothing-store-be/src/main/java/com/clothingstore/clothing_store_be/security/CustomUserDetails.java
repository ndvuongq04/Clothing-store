package com.clothingstore.clothing_store_be.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.clothingstore.clothing_store_be.entity.User;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String password;
    private final String role;
    private final boolean status;

    public CustomUserDetails(User user) {
        this.id = user.getUserId(); // Long
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.status = user.isStatus();
    }

    // VD: role = "admin" -> "ROLE_ADMIN"
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    // false -> Spring tu dong throw LockedException
    @Override
    public boolean isAccountNonLocked() {
        return status;
    }

    // false -> Spring tu dong throw DisabledException
    @Override
    public boolean isEnabled() {
        return status;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}

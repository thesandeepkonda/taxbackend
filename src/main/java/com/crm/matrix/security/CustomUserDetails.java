package com.crm.matrix.security;

import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        Role role = user.getRole();
        if (role == null) {
            return authorities;
        }

        // 1. Add Role Authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        // 2. Safe check for permissions to avoid LazyInitializationException
        try {
            if (user.getPermissions() != null) {
                for (Permission permission : user.getPermissions()) {
                    if (permission != null && Boolean.TRUE.equals(permission.getActive())) {
                        authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                    }
                }
            }
        } catch (Exception e) {
            // Fallback if collection is uninitialized outside transaction
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmployeeCode();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getActive());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getActive());
    }
}
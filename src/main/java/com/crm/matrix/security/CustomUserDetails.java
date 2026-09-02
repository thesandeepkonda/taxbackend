package com.crm.matrix.security;

import com.crm.matrix.entity.Permission;
import com.crm.matrix.entity.Role;
import com.crm.matrix.entity.User;
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

        if (!Boolean.TRUE.equals(role.getActive())) {
            return authorities;
        }


        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));


        if (role.getPermissions() == null) {
            return authorities;
        }

        for (Permission permission : role.getPermissions()) {

            if (permission == null) {
                continue;
            }

            if (!Boolean.TRUE.equals(permission.getActive())) {
                continue;
            }

            authorities.add(new SimpleGrantedAuthority(permission.getCode()));
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
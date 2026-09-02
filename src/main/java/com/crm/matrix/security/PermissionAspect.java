package com.crm.matrix.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(hasPermission)")
    public void checkPermission(JoinPoint joinPoint, HasPermission hasPermission) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {

            throw new AccessDeniedException("User is not authenticated");
        }


        String requiredPermission = hasPermission.value();



        boolean permissionGranted = authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(requiredPermission));



        if (!permissionGranted) {

            throw new AccessDeniedException("Access denied. Required permission: " + requiredPermission);
        }
    }
}
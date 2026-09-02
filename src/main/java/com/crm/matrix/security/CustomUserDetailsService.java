package com.crm.matrix.security;

import com.crm.matrix.entity.User;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;



    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String employeeCode) throws UsernameNotFoundException {

        User user = userRepository.findByEmployeeCode(employeeCode).orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + employeeCode));

        return new CustomUserDetails(user);
    }
}
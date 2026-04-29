package com.lendos.identity.service;

import com.lendos.identity.entity.User;
import com.lendos.identity.repository.UserRepository;
import com.lendos.identity.security.LendosUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ENGLISH) : email;
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", normalizedEmail);
                    return new UsernameNotFoundException("User not found: " + normalizedEmail);
                });
        return new LendosUserDetails(user);
    }
}

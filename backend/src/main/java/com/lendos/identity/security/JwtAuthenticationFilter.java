package com.lendos.identity.security;

import com.lendos.identity.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        List<String> headerNames = Collections.list(request.getHeaderNames());
        log.debug("JWT filter request: method={} path={} headers={}",
                request.getMethod(), request.getRequestURI(), headerNames);

        final String jwt = extractTokenFromHeader(authHeader);
        if (!StringUtils.hasText(jwt)) {
            if (StringUtils.hasText(authHeader)) {
                log.debug("Authorization header present but token extraction failed: rawHeader='{}'", authHeader);
            }
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("Extracted JWT token preview: {}", tokenPreview(jwt));

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            log.debug("JWT subject extracted: {}", userEmail);

            if (StringUtils.hasText(userEmail)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.debug("JWT validation passed for user {}", userEmail);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Enrich MDC with authenticated user info for logging
                    MDC.put("userId", userEmail);
                } else {
                    log.warn("JWT validation failed for user {} with token {}", userEmail, tokenPreview(jwt));
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: type={} message={} token={} correlationId={}",
                    e.getClass().getSimpleName(), e.getMessage(), tokenPreview(jwt), MDC.get("correlationId"));
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        String normalized = authHeader.trim();
        if (!normalized.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String[] parts = normalized.split("\\s+", 2);
        if (parts.length < 2 || !StringUtils.hasText(parts[1])) {
            return null;
        }
        return parts[1].trim();
    }

    private String tokenPreview(String token) {
        int previewLength = Math.min(token.length(), 20);
        return token.substring(0, previewLength) + (token.length() > previewLength ? "..." : "");
    }
}

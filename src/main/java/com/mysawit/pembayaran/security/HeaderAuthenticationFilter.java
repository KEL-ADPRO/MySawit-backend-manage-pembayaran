package com.mysawit.pembayaran.security;

import com.mysawit.pembayaran.model.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawUserId = request.getHeader(USER_ID_HEADER);
        String rawRole = request.getHeader(USER_ROLE_HEADER);

        if (rawUserId == null && rawRole == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (rawUserId == null || rawRole == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Both X-User-Id and X-User-Role are required");
            return;
        }

        try {
            UUID userId = UUID.fromString(rawUserId);
            UserRole role = UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
            AuthenticatedUser user = new AuthenticatedUser(userId, role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication headers");
        }
    }
}

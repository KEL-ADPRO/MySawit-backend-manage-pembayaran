package com.mysawit.pembayaran.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/pembayaran/wallet/topup/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pembayaran/wallet/topup/mock-pay/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pembayaran/wallet/topup/mock-pay/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pembayaran/payroll").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pembayaran/payroll/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pembayaran/payroll/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pembayaran/wage-config").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pembayaran/wallet/topup").hasRole("ADMIN")
                        .requestMatchers("/internal/payroll-events/**").hasRole("ADMIN")
                        .requestMatchers("/api/pembayaran/payroll/**").authenticated()
                        .requestMatchers("/api/pembayaran/wallet/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/pembayaran/wallet").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                response.sendError(401, "Authentication is required");
    }
}

package com.wedbackendservice.nextgen.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/v1/auth/login-initiate", "/api/v1/auth/login-verify-otp", "/api/v1/auth/login-resend-otp",
            "/api/v1/auth/register-initiate", "/api/v1/auth/register-verify-otp", "/api/v1/auth/register-resend-otp",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/forgot-resend-otp",
            "/api/v1/auth/refresh-token", "/api/v1/auth/logout"
    };

    @Bean
    public SecurityFilterChain filterchain(HttpSecurity httpSecurity) {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/brands", "/api/v1/brands/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shipments/**").permitAll()
                        .anyRequest().permitAll());
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

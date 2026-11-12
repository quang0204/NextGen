package com.wedbackendservice.nextgen.configuration;

import com.wedbackendservice.nextgen.service.shared.TokenRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/v1/auth/login-initiate", "/api/v1/auth/login-verify-otp", "/api/v1/auth/login-resend-otp",
            "/api/v1/auth/register-initiate", "/api/v1/auth/register-verify-otp", "/api/v1/auth/register-resend-otp",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password", "/api/v1/auth/forgot-resend-otp",
            "/api/v1/auth/refresh-token", "/api/v1/auth/logout"
    };
    @Value("${jwt.signed-key}")
    private String signedKey;
    @Value("${app.cors.allowed-origins}")
    List<String> allowedOrigins;

    private final TokenRedisService tokenRedisService;
    @Bean
    public SecurityFilterChain filterchain(HttpSecurity httpSecurity) {
        httpSecurity
                .cors(cors->cors.configurationSource(corsConfigurationSource(allowedOrigins)))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/brands", "/api/v1/brands/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/faqs", "/api/v1/faqs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shipments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/promotions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/vouchers/valid").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/v1/storage/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*/images/*").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return httpSecurity.build();
    }
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(signedKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"))
                .macAlgorithm(MacAlgorithm.HS512).build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                jwt -> "access".equals(jwt.getClaimAsString("token_type"))
                        && !tokenRedisService.isTokenBlacklisted(Long.valueOf(jwt.getSubject()), jwt.getTokenValue())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        return jwtDecoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("Scope");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource(List<String> origins) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(origins);
        corsConfiguration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        corsConfiguration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

}

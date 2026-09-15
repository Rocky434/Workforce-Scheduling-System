package com.example.scheduling.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtKey(@Value("${app.jwt-secret}") String secret) {
        if (secret.length() < 43 || secret.startsWith("change-this") || secret.startsWith("請使用")) {
            throw new IllegalStateException("JWT_SECRET must be a random secret of at least 32 bytes");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        var jwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();
        OAuth2TokenValidator<Jwt> tokenTypeValidator = jwt -> "access".equals(jwt.getClaimAsString("token_type"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Only access tokens may authorize API requests", null));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("scheduling"), tokenTypeValidator));
        return jwtDecoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                jwt -> List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))));
        return authenticationConverter;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtAuthenticationConverter authenticationConverter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {
        }).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/login", "/api/auth/refresh",
                        "/api/auth/logout", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(authenticationConverter))).build();
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors-origin}") String origin) {
        var corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(List.of(origin));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        corsConfiguration.setAllowCredentials(true);
        var corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return corsConfigurationSource;
    }
}

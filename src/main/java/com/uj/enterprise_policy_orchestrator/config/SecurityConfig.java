package com.uj.enterprise_policy_orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/policies")
                    .hasRole("admin")
                    .requestMatchers(HttpMethod.PATCH, "/api/policies/*/expiration")
                    .hasRole("admin")
                    .requestMatchers(HttpMethod.POST, "/api/categories")
                    .hasAnyRole("admin", "manager")
                    .requestMatchers(HttpMethod.PUT, "/api/categories/*")
                    .hasAnyRole("admin", "manager")
                    .requestMatchers(HttpMethod.DELETE, "/api/categories/*")
                    .hasAnyRole("admin", "manager")
                    .requestMatchers(HttpMethod.POST, "/api/expense-requests")
                    .hasAnyRole("employee", "admin")
                    .requestMatchers("/api/expense-requests/review/**")
                    .hasAnyRole("manager", "admin")
                    .requestMatchers("/api/expense-requests/review")
                    .hasAnyRole("manager", "admin")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt ->
            JwtRoleExtractor.extractRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.authorityName()))
                .map(GrantedAuthority.class::cast)
                .toList());
    return converter;
  }
}

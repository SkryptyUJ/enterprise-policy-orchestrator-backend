package com.uj.enterprise_policy_orchestrator.integration;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class IntegrationTestConfiguration {

  static final String TEST_BEARER_TOKEN = "test-token";

  @Bean
  @ServiceConnection
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password");
  }

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate =
        new RestTemplate(new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));
    restTemplate
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              if (request.getHeaders().getFirst("Authorization") == null) {
                request.getHeaders().setBearerAuth(TEST_BEARER_TOKEN);
              }
              return execution.execute(request, body);
            });
    return restTemplate;
  }

  /**
   * Replaces the real Auth0-backed {@link JwtDecoder} for integration tests so we don't make
  * network calls to Auth0. Accepts any non-empty token and produces a {@link Jwt} whose subject
  * matches the token value.
   */
  @Bean
  @Primary
  public JwtDecoder testJwtDecoder() {
    return token -> {
      Instant now = Instant.now();
      return new Jwt(
          token,
          now,
          now.plusSeconds(3600),
          Map.of("alg", "none"),
          Map.of(
              "sub", token,
              "iss", "https://test-issuer/",
              "aud", List.of("test-audience"),
              "scope", "read write"));
    };
  }
}

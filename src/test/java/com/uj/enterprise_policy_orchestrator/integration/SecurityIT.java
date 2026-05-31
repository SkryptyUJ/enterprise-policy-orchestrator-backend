package com.uj.enterprise_policy_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.uj.enterprise_policy_orchestrator.policy.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.policy.dto.PolicyDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@DisplayName("Security Filter Chain E2E Tests")
class SecurityIT extends AbstractIntegrationTest {

  // Bare RestTemplate WITHOUT the auth interceptor from IntegrationTestConfiguration so we can
  // exercise the unauthenticated path through the security filter chain.
  private final RestTemplate anonymousRestTemplate = new RestTemplate();

  @Test
  @DisplayName("GET protected endpoint without Authorization header returns 401")
  void shouldReturn401WhenNoAuthorizationHeaderProvided() {
    HttpClientErrorException.Unauthorized ex =
        assertThrows(
            HttpClientErrorException.Unauthorized.class,
            () ->
          anonymousRestTemplate.getForEntity(baseUrl() + "/api/policies", PolicyDto[].class));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  @DisplayName("POST protected endpoint without Authorization header returns 401")
  void shouldReturn401OnPostWithoutAuthorizationHeader() {
    CreatePolicyDto request =
        new CreatePolicyDto(
            Optional.empty(),
            1,
            "Policy",
            "desc",
            LocalDateTime.now(),
            null,
            new BigDecimal("100"),
            new BigDecimal("1000"),
            "Travel",
            1);
    HttpClientErrorException.Unauthorized ex =
        assertThrows(
            HttpClientErrorException.Unauthorized.class,
            () ->
                anonymousRestTemplate.postForEntity(
            baseUrl() + "/api/policies",
                    request,
            PolicyDto.class));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  @DisplayName("Request with Bearer token is authenticated and reaches the controller")
  void shouldAuthenticateRequestWithBearerToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(IntegrationTestConfiguration.TEST_BEARER_TOKEN);
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<PolicyDto[]> response =
        anonymousRestTemplate.exchange(
        baseUrl() + "/api/policies",
            HttpMethod.GET,
            new HttpEntity<>(headers),
        PolicyDto[].class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  @DisplayName("CORS preflight OPTIONS request is permitted without authentication")
  void shouldAllowCorsPreflightWithoutAuthentication() {
    HttpHeaders headers = new HttpHeaders();
    headers.setOrigin("http://localhost:3000");
    headers.setAccessControlRequestMethod(HttpMethod.GET);
    headers.add("Access-Control-Request-Headers", "authorization,content-type");
    ResponseEntity<Void> response =
        anonymousRestTemplate.exchange(
        baseUrl() + "/api/policies",
            HttpMethod.OPTIONS,
            new HttpEntity<>(headers),
        Void.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}

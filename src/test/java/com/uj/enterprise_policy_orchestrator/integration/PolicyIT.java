package com.uj.enterprise_policy_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@DisplayName("Policy Controller E2E Tests")
class PolicyIT extends AbstractIntegrationTest {
  @Autowired private RestTemplate restTemplate;
  @Autowired private PolicyRepository policyRepository;

  @BeforeEach
  void setUp() {
    policyRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    policyRepository.deleteAll();
  }

  @Nested
  @DisplayName("POST /api/users/{userId}/policies - Create Policy")
  class CreatePolicyE2E {
    @Test
    @DisplayName("should create a new policy and persist to database")
    void shouldCreatePolicyAndPersistToDatabase() {
      String userId = "user-123";
      LocalDateTime startsAt = LocalDateTime.of(2026, 4, 1, 0, 0, 0);
      LocalDateTime expiresAt = LocalDateTime.of(2027, 3, 31, 23, 59, 59);
      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.empty(),
              1,
              "Travel Policy",
              "Company travel policy for employees",
              startsAt,
              expiresAt,
              new BigDecimal("100"),
              new BigDecimal("5000"),
              "1",
              2);
      var beforeCount = policyRepository.count();
      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      PolicyDto body = response.getBody();
      assertEquals(userId, body.authorUserId());
      assertEquals("Travel Policy", body.name());
      assertEquals(1, body.version());
      var afterCount = policyRepository.count();
      assertEquals(beforeCount + 1, afterCount);
    }

    @Test
    @DisplayName("should generate policyId if not provided")
    void shouldGeneratePolicyIdIfNotProvided() {
      String userId = "user-456";
      LocalDateTime startsAt = LocalDateTime.now();
      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.empty(),
              2,
              "Hardware Policy",
              "Company equipment policy",
              startsAt,
              null,
              new BigDecimal("500"),
              new BigDecimal("10000"),
              "2",
              3);
      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getBody().policyId());
      assertFalse(response.getBody().policyId().isEmpty());
    }

    @Test
    @DisplayName("should use provided policyId")
    void shouldUseProvidedPolicyId() {
      String userId = "user-789";
      String providedPolicyId = "CUSTOM-POL-001";
      LocalDateTime startsAt = LocalDateTime.now();
      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.of(providedPolicyId),
              3,
              "Custom Policy",
              "Custom policy",
              startsAt,
              null,
              null,
              null,
              "3",
              1);
      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getBody().policyId());
      assertEquals(providedPolicyId, response.getBody().policyId());
    }

    @Test
    @DisplayName("should create policy v2 when updating existing policy")
    void shouldCreatePolicyV2WhenUpdatingExisting() {
      String userId = "user-update";
      String policyId = "POLICY-UPDATE-001";
      LocalDateTime startsAtV1 = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      LocalDateTime startsAtV2 = LocalDateTime.of(2026, 6, 1, 0, 0, 0);

      CreatePolicyDto v1Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Policy V1",
              "Version 1",
              startsAtV1,
              null,
              new BigDecimal("100"),
              new BigDecimal("1000"),
              "1",
              1);
      ResponseEntity<PolicyDto> v1Response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", v1Request, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, v1Response.getStatusCode());
      assertNotNull(v1Response.getBody());
      assertEquals(1, v1Response.getBody().version());

      CreatePolicyDto v2Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Policy V2",
              "Version 2",
              startsAtV2,
              null,
              new BigDecimal("200"),
              new BigDecimal("2000"),
              "1",
              2);
      ResponseEntity<PolicyDto> v2Response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", v2Request, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, v2Response.getStatusCode());
      assertNotNull(v2Response.getBody());
      assertEquals(2, v2Response.getBody().version());
      assertEquals(policyId, v2Response.getBody().policyId());
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/policies/{policyId} - Get Single Policy")
  class GetPolicyByIdE2E {
    @Test
    @DisplayName("should retrieve policy by policyId from database")
    void shouldRetrievePolicyById() {
      String userId = "user-retrieve";
      String policyId = "POL-RETRIEVE-001";
      LocalDateTime startsAt = LocalDateTime.now();

      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Retrieve Test Policy",
              "Test policy for retrieval",
              startsAt,
              null,
              new BigDecimal("100"),
              new BigDecimal("5000"),
              "1",
              1);
      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);

      ResponseEntity<PolicyDto> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}",
              PolicyDto.class,
              userId,
              policyId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(policyId, response.getBody().policyId());
      assertEquals("Retrieve Test Policy", response.getBody().name());
      assertEquals(userId, response.getBody().authorUserId());
      assertEquals(1, response.getBody().version());
    }

    @Test
    @DisplayName("should return latest version when multiple versions exist")
    void shouldReturnLatestVersionWhenMultipleVersionsExist() {
      String userId = "user-versions";
      String policyId = "POL-VERSIONS-001";
      LocalDateTime startsAtV1 = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      LocalDateTime startsAtV2 = LocalDateTime.of(2026, 3, 1, 0, 0, 0);
      LocalDateTime startsAtV3 = LocalDateTime.of(2026, 5, 1, 0, 0, 0);

      CreatePolicyDto v1Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Policy V1",
              "V1",
              startsAtV1,
              null,
              new BigDecimal("100"),
              new BigDecimal("1000"),
              "1",
              1);
      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/policies", v1Request, PolicyDto.class, userId);

      CreatePolicyDto v2Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Policy V2",
              "V2",
              startsAtV2,
              null,
              new BigDecimal("200"),
              new BigDecimal("2000"),
              "1",
              2);
      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/policies", v2Request, PolicyDto.class, userId);

      CreatePolicyDto v3Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Policy V3",
              "V3",
              startsAtV3,
              null,
              new BigDecimal("300"),
              new BigDecimal("3000"),
              "1",
              3);
      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/policies", v3Request, PolicyDto.class, userId);

      ResponseEntity<PolicyDto> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}",
              PolicyDto.class,
              userId,
              policyId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(3, response.getBody().version());
      assertEquals("Policy V3", response.getBody().name());
      assertEquals("V3", response.getBody().description());
    }

    @Test
    @DisplayName("should return 404 when policy does not exist")
    void shouldReturn404WhenPolicyNotFound() {
      String userId = "user-notfound";
      String nonExistentPolicyId = "NONEXISTENT-POL-001";
      ResponseEntity<PolicyDto> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}",
              PolicyDto.class,
              userId,
              nonExistentPolicyId);
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/policies/{policyId}/history - Get Policy History")
  class GetPolicyHistoryE2E {
    @Test
    @DisplayName("should retrieve complete policy history ordered by version desc")
    void shouldRetrievePolicyHistoryOrderedByVersionDesc() {
      String userId = "user-history";
      String policyId = "POL-HISTORY-001";
      LocalDateTime startsAtV1 = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      LocalDateTime startsAtV2 = LocalDateTime.of(2026, 3, 1, 0, 0, 0);
      LocalDateTime startsAtV3 = LocalDateTime.of(2026, 5, 1, 0, 0, 0);

      for (int i = 1; i <= 3; i++) {
        LocalDateTime startDate =
            switch (i) {
              case 1 -> startsAtV1;
              case 2 -> startsAtV2;
              default -> startsAtV3;
            };
        String desc = "Version " + i;
        CreatePolicyDto request =
            new CreatePolicyDto(
                Optional.of(policyId),
                1,
                "Policy V" + i,
                desc,
                startDate,
                null,
                new BigDecimal(String.valueOf(100 * i)),
                new BigDecimal(String.valueOf(1000 * i)),
                "Travel",
                i);
        restTemplate.postForEntity(
            baseUrl() + "/api/users/{userId}/policies", request, PolicyDto.class, userId);
      }

      ResponseEntity<PolicyDto[]> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}/history",
              PolicyDto[].class,
              userId,
              policyId);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      PolicyDto[] history = response.getBody();
      assertNotNull(history);
      assertEquals(3, history.length);
      assertEquals(3, history[0].version());
      assertEquals("Version 3", history[0].description());
      assertEquals(2, history[1].version());
      assertEquals("Version 2", history[1].description());
      assertEquals(1, history[2].version());
      assertEquals("Version 1", history[2].description());
    }

    @Test
    @DisplayName("should return 404 when policy history does not exist")
    void shouldReturn404WhenHistoryNotFound() {
      String userId = "user-history-notfound";
      String nonExistentPolicyId = "NONEXISTENT-HISTORY-001";
      ResponseEntity<PolicyDto[]> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}/history",
              PolicyDto[].class,
              userId,
              nonExistentPolicyId);
      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Database Persistence")
  class EdgeCasesAndDatabasePersistence {
    @Test
    @DisplayName("should handle policy with null expiresAt (indefinite policy)")
    void shouldHandleNullExpiresAt() {
      String userId = "user-indefinite";
      String policyId = "INDEFINITE-POL-001";
      LocalDateTime startsAt = LocalDateTime.now();
      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Indefinite Policy",
              "Policy with no expiration",
              startsAt,
              null, // No expiration
              new BigDecimal("100"),
              new BigDecimal("5000"),
              "1",
              1);
      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNull(response.getBody().expiresAt());
    }

    @Test
    @DisplayName("should handle policy with null price bounds (unbounded policy)")
    void shouldHandleNullPriceBounds() {
      String userId = "user-unbounded";
      String policyId = "UNBOUNDED-POL-001";
      LocalDateTime startsAt = LocalDateTime.now();
      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.of(policyId),
              2,
              "Unbounded Price Policy",
              "No price constraints",
              startsAt,
              null,
              null,
              null,
              "2",
              1);
      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);
      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNull(response.getBody().minPrice());
      assertNull(response.getBody().maxPrice());
    }

    @Test
    @DisplayName("should preserve policy immutability (non-updatable fields)")
    void shouldPreservePolicyImmutability() {
      String userId = "user-immutable";
      String policyId = "IMMUTABLE-POL-001";
      LocalDateTime startsAt = LocalDateTime.now();
      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Original Name",
              "Original Description",
              startsAt,
              null,
              new BigDecimal("100"),
              new BigDecimal("5000"),
              "1",
              1);
      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);

      PolicyDto createdPolicy = response.getBody();
      String originalPolicyId = Objects.requireNonNull(createdPolicy).policyId();
      String originalName = createdPolicy.name();
      Integer originalVersion = createdPolicy.version();

      ResponseEntity<PolicyDto> retrievedResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}",
              PolicyDto.class,
              userId,
              policyId);

      assertNotNull(retrievedResponse.getBody());
      assertEquals(originalPolicyId, retrievedResponse.getBody().policyId());
      assertEquals(originalName, retrievedResponse.getBody().name());
      assertEquals(originalVersion, retrievedResponse.getBody().version());
    }

    @Test
    @DisplayName("should correctly handle policy versioning with exact date transitions")
    void shouldHandleExactDateTransitions() {
      String userId = "user-date-transition";
      String policyId = "DATE-TRANSITION-001";
      LocalDateTime v1StartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      LocalDateTime v2StartsAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);

      CreatePolicyDto v1Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "V1",
              "V1",
              v1StartsAt,
              null,
              new BigDecimal("100"),
              new BigDecimal("5000"),
              "1",
              1);
      ResponseEntity<PolicyDto> v1Response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", v1Request, PolicyDto.class, userId);

      CreatePolicyDto v2Request =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "V2",
              "V2",
              v2StartsAt,
              null,
              new BigDecimal("200"),
              new BigDecimal("5000"),
              "1",
              1);
      ResponseEntity<PolicyDto> v2Response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", v2Request, PolicyDto.class, userId);

      ResponseEntity<PolicyDto[]> historyResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/policies/{policyId}/history",
              PolicyDto[].class,
              userId,
              policyId);

      PolicyDto[] history = historyResponse.getBody();
      assertNotNull(history);
      assertEquals(2, history.length);
      assertEquals(v2StartsAt, history[1].expiresAt());
      assertNull(history[0].expiresAt());
    }

    @Test
    @DisplayName("should handle large policy amounts correctly")
    void shouldHandleLargeAmounts() {
      String userId = "user-large-amount";
      String policyId = "LARGE-AMOUNT-POL-001";
      LocalDateTime startsAt = LocalDateTime.now();
      BigDecimal largeMin = new BigDecimal("999999999");
      BigDecimal largeMax = new BigDecimal("999999999999999");

      CreatePolicyDto createRequest =
          new CreatePolicyDto(
              Optional.of(policyId),
              1,
              "Large Amount Policy",
              "Policy for large expenses",
              startsAt,
              null,
              largeMin,
              largeMax,
              "1",
              1);

      ResponseEntity<PolicyDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/policies", createRequest, PolicyDto.class, userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(largeMin, response.getBody().minPrice());
      assertEquals(largeMax, response.getBody().maxPrice());
    }

    @Test
    @DisplayName("should maintain policy count consistency across operations")
    void shouldMaintainPolicyCountConsistency() {
      String userId = "user-count-test";
      long initialCount = policyRepository.count();

      for (int i = 0; i < 5; i++) {
        CreatePolicyDto request =
            new CreatePolicyDto(
                Optional.of("POLICY-COUNT-" + i),
                i + 1,
                "Policy " + i,
                "Description " + i,
                LocalDateTime.now(),
                null,
                new BigDecimal("100"),
                new BigDecimal("5000"),
                i + "1",
                1);
        restTemplate.postForEntity(
            baseUrl() + "/api/users/{userId}/policies", request, PolicyDto.class, userId);
      }

      long afterCreation = policyRepository.count();
      assertEquals(initialCount + 5, afterCreation);

      for (int i = 0; i < 5; i++) {
        ResponseEntity<PolicyDto> response =
            restTemplate.getForEntity(
                baseUrl() + "/api/users/{userId}/policies/{policyId}",
                PolicyDto.class,
                userId,
                "POLICY-COUNT-" + i);
        assertEquals(HttpStatus.OK, response.getStatusCode());
      }
    }
  }
}

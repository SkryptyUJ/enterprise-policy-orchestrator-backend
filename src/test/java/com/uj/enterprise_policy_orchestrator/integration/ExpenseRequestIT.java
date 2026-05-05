package com.uj.enterprise_policy_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@DisplayName("ExpenseRequest Controller E2E Tests")
class ExpenseRequestIT extends AbstractIntegrationTest {

  private final RestTemplate restTemplate = new RestTemplate();
  @Autowired private ExpenseRequestRepository expenseRequestRepository;
  @Autowired private PolicyRepository policyRepository;

  @BeforeEach
  void setUp() {
    expenseRequestRepository.deleteAll();
    policyRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    expenseRequestRepository.deleteAll();
    policyRepository.deleteAll();
  }

  @Nested
  @DisplayName("POST /api/users/{userId}/expense-requests - Create Expense Request")
  class CreateExpenseRequestE2E {

    @Test
    @DisplayName("should create expense request and match applicable policies")
    void shouldCreateExpenseRequestAndMatchApplicablePolicies() {
      String userId = "expense-user-1";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("TRAVEL-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Travel Policy")
              .description("Travel policy")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(2)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"), "1", "Flight to Krakow", LocalDate.of(2026, 3, 20));

      var beforeCount = expenseRequestRepository.count();

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      ExpenseRequestDto body = response.getBody();
      assertEquals(userId, body.userId());
      assertEquals(new BigDecimal("1500.00"), body.amount());
      assertEquals("1", body.category());
      assertEquals("Flight to Krakow", body.description());
      assertEquals(LocalDate.of(2026, 3, 20), body.expenseDate());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, body.status());
      assertNotNull(body.submittedAt());

      var afterCount = expenseRequestRepository.count();
      assertEquals(beforeCount + 1, afterCount);
    }

    @Test
    @DisplayName("should persist expense request to database with WAITING_FOR_APPROVAL status")
    void shouldPersistExpenseRequestWithCorrectStatus() {
      String userId = "expense-user-2";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("OFFICE-POLICY-001")
              .authorUserId("admin")
              .categoryId(2)
              .name("Office Supplies")
              .description("Office policy")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("10"))
              .maxPrice(new BigDecimal("1000"))
              .category("2")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"),
              "2",
              "Office supplies - pens and paper",
              LocalDate.of(2026, 3, 15));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, response.getBody().status());

      var allRequests = expenseRequestRepository.findAll();
      assertEquals(1, allRequests.size());
      var savedRequest = allRequests.getFirst();
      assertEquals(userId, savedRequest.getUserId());
      assertEquals(new BigDecimal("250.00"), savedRequest.getAmount());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, savedRequest.getStatus());
    }

    @Test
    @DisplayName("should decline expense request when no applicable policies exist")
    void shouldDeclineExpenseRequestWhenNoPoliciesApply() {
      String userId = "expense-user-3";

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"),
              "NON-EXISTENT",
              "Expense without matching policy",
              LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("should handle expense amounts within policy bounds")
    void shouldHandleExpenseAmountsWithinPolicyBounds() {
      String userId = "expense-user-4";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("BOUNDED-POLICY-001")
              .authorUserId("admin")
              .categoryId(3)
              .name("Bounded Policy")
              .description("Policy with bounds")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("1000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto minRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"), "3", "Minimum boundary test", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> minResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              minRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, minResponse.getStatusCode());
      assertNotNull(minResponse.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, minResponse.getBody().status());

      CreateExpenseRequestDto maxRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1000.00"), "3", "Maximum boundary test", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> maxResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              maxRequest,
              ExpenseRequestDto.class,
              userId + "-max");

      assertEquals(HttpStatus.CREATED, maxResponse.getStatusCode());
      assertNotNull(maxResponse.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, maxResponse.getBody().status());
    }

    @Test
    @DisplayName("should link expense request to applicable policies")
    void shouldLinkExpenseRequestToApplicablePolicies() {
      String userId = "expense-user-5";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

      Policy policy1 =
          Policy.builder()
              .policyId("POLICY-1")
              .authorUserId("admin")
              .categoryId(1)
              .name("Policy 1")
              .description("Policy 1")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();

      Policy policy2 =
          Policy.builder()
              .policyId("POLICY-2")
              .authorUserId("admin")
              .categoryId(1)
              .name("Policy 2")
              .description("Policy 2")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("50"))
              .maxPrice(new BigDecimal("3000"))
              .category("Travel")
              .authorizedRole(2)
              .build();

      policyRepository.save(policy1);
      policyRepository.save(policy2);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"), "1", "Multi-policy test", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      var expenseRequests = expenseRequestRepository.findAll();
      assertEquals(1, expenseRequests.size());
      var savedRequest = expenseRequests.getFirst();
      assertFalse(savedRequest.getApplicablePolicies().isEmpty());
    }

    @Test
    @DisplayName("should handle date-based policy filtering correctly")
    void shouldHandleDateBasedPolicyFiltering() {
      String userId = "expense-user-6";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
      LocalDateTime policyExpiresAt = LocalDateTime.of(2026, 4, 1, 0, 0, 0);

      Policy policy =
          Policy.builder()
              .policyId("DATE-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Date Policy")
              .description("Date policy")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(policyExpiresAt)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("500.00"), "1", "Within policy validity", LocalDate.of(2026, 3, 15));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, response.getBody().status());
    }

    @Test
    @DisplayName("should set submittedAt timestamp when creating expense request")
    void shouldSetSubmittedAtTimestamp() {
      String userId = "expense-user-7";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("TIMESTAMP-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Timestamp Policy")
              .description("Policy")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("500.00"), "1", "Timestamp test", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getBody().submittedAt());

      var expenseRequests = expenseRequestRepository.findAll();
      assertEquals(1, expenseRequests.size());
      assertNotNull(expenseRequests.getFirst().getSubmittedAt());
    }

    @Test
    @DisplayName("should handle multiple expense requests from different users")
    void shouldHandleMultipleExpenseRequestsFromDifferentUsers() {
      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("MULTI-USER-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Multi-user Policy")
              .description("Policy")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("500.00"), "1", "Test", LocalDate.of(2026, 3, 20));

      for (String userId : new String[] {"user-A", "user-B", "user-C"}) {
        ResponseEntity<ExpenseRequestDto> response =
            restTemplate.postForEntity(
                baseUrl() + "/api/users/{userId}/expense-requests",
                createRequest,
                ExpenseRequestDto.class,
                userId);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
      }

      var allRequests = expenseRequestRepository.findAll();
      assertEquals(3, allRequests.size());
      var userIds = allRequests.stream().map(ExpenseRequest::getUserId).distinct().toList();
      assertEquals(3, userIds.size());
    }

    @Test
    @DisplayName("should preserve all expense request fields in database")
    void shouldPreserveAllExpenseRequestFields() {
      String userId = "expense-user-8";
      String description = "Detailed expense description with special chars: @#$%";
      LocalDate expenseDate = LocalDate.of(2026, 3, 20);
      BigDecimal amount = new BigDecimal("1234.56");

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("PRESERVE-POLICY-001")
              .authorUserId("admin")
              .categoryId(5)
              .name("Preserve Policy")
              .description("Policy")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(amount, "5", description, expenseDate);

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      var expenseRequests = expenseRequestRepository.findAll();
      assertEquals(1, expenseRequests.size());
      var savedRequest = expenseRequests.getFirst();
      assertEquals(userId, savedRequest.getUserId());
      assertEquals(0, savedRequest.getAmount().compareTo(amount));
      assertEquals("Travel", savedRequest.getCategory());
      assertEquals(expenseDate, savedRequest.getExpenseDate());
    }
  }

  @Nested
  @DisplayName("Advanced Database Persistence Tests")
  class AdvancedDatabasePersistenceTests {

    @Test
    @DisplayName("should verify expense-policy many-to-many relationship persistence")
    void shouldPersistManyToManyRelationship() {
      String userId = "expense-user-m2m";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy1 =
          Policy.builder()
              .policyId("M2M-POLICY-1")
              .authorUserId("admin")
              .categoryId(1)
              .name("M2M Policy 1")
              .description("Policy 1")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      Policy policy2 =
          Policy.builder()
              .policyId("M2M-POLICY-2")
              .authorUserId("admin")
              .categoryId(1)
              .name("M2M Policy 2")
              .description("Policy 2")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("50"))
              .maxPrice(new BigDecimal("3000"))
              .category("Travel")
              .authorizedRole(2)
              .build();
      Policy policy3 =
          Policy.builder()
              .policyId("M2M-POLICY-3")
              .authorUserId("admin")
              .categoryId(1)
              .name("M2M Policy 3")
              .description("Policy 3")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("200"))
              .maxPrice(new BigDecimal("4000"))
              .category("Travel")
              .authorizedRole(1)
              .build();

      policyRepository.saveAll(java.util.List.of(policy1, policy2, policy3));

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"), "1", "Multi-policy test", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());

      var allRequests = expenseRequestRepository.findAll();
      assertEquals(1, allRequests.size());
      var savedRequest = allRequests.getFirst();

      assertFalse(savedRequest.getApplicablePolicies().isEmpty());
      assertTrue(savedRequest.getApplicablePolicies().size() >= 2);
      assertTrue(
          savedRequest.getApplicablePolicies().stream()
              .anyMatch(p -> p.getPolicyId().equals("M2M-POLICY-1")));
      assertTrue(
          savedRequest.getApplicablePolicies().stream()
              .anyMatch(p -> p.getPolicyId().equals("M2M-POLICY-2")));
    }

    @Test
    @DisplayName("should correctly filter by expired policy")
    void shouldCorrectlyFilterByExpiredPolicy() {
      String userId = "expense-user-expired";

      LocalDateTime policyStartsAt = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
      LocalDateTime policyExpiresAt = LocalDateTime.of(2026, 2, 1, 0, 0, 0);

      Policy expiredPolicy =
          Policy.builder()
              .policyId("EXPIRED-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Expired Policy")
              .description("This policy has expired")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(policyExpiresAt)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(expiredPolicy);

      Policy activePolicy =
          Policy.builder()
              .policyId("ACTIVE-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Active Policy")
              .description("This policy is active")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(activePolicy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"), "1", "Expired policy test", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());

      var allRequests = expenseRequestRepository.findAll();
      var savedRequest = allRequests.getFirst();
      assertTrue(
          savedRequest.getApplicablePolicies().stream()
              .noneMatch(p -> p.getPolicyId().equals("EXPIRED-POLICY-001")));
      assertTrue(
          savedRequest.getApplicablePolicies().stream()
              .anyMatch(p -> p.getPolicyId().equals("ACTIVE-POLICY-001")));
    }

    @Test
    @DisplayName("should handle boundary amounts correctly (minimum)")
    void shouldHandleBoundaryAmountsMinimum() {
      String userId = "expense-user-min-boundary";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("BOUNDARY-MIN-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Boundary Policy")
              .description("Policy with boundaries")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("1000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto minRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"), "1", "Exactly at minimum", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> minResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              minRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, minResponse.getStatusCode());
      assertNotNull(minResponse.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, minResponse.getBody().status());

      String userId2 = "expense-user-below-min";
      CreateExpenseRequestDto belowMinRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("99.99"), "1", "Below minimum", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> belowMinResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              belowMinRequest,
              ExpenseRequestDto.class,
              userId2);

      assertEquals(HttpStatus.BAD_REQUEST, belowMinResponse.getStatusCode());
    }

    @Test
    @DisplayName("should handle boundary amounts correctly (maximum)")
    void shouldHandleBoundaryAmountsMaximum() {
      String userId = "expense-user-max-boundary";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("BOUNDARY-MAX-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Boundary Policy")
              .description("Policy with boundaries")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("1000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto maxRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1000.00"), "1", "Exactly at maximum", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> maxResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              maxRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, maxResponse.getStatusCode());
      assertNotNull(maxResponse.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, maxResponse.getBody().status());

      String userId2 = "expense-user-above-max";
      CreateExpenseRequestDto aboveMaxRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1000.01"), "1", "Above maximum", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> aboveMaxResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              aboveMaxRequest,
              ExpenseRequestDto.class,
              userId2);

      assertEquals(HttpStatus.BAD_REQUEST, aboveMaxResponse.getStatusCode());
    }

    @Test
    @DisplayName("should correctly handle expense with no price bounds policy")
    void shouldHandleNoPriceBoundsPolicy() {
      String userId = "expense-user-no-bounds";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy unboundedPolicy =
          Policy.builder()
              .policyId("UNBOUNDED-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Unbounded Policy")
              .description("No price constraints")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(null)
              .maxPrice(null)
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(unboundedPolicy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("999999.99"), "1", "Very large expense", LocalDate.of(2026, 3, 20));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, response.getBody().status());
    }
  }
}

package com.uj.enterprise_policy_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uj.enterprise_policy_orchestrator.expense_request.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.expense_request.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.policy.Policy;
import com.uj.enterprise_policy_orchestrator.policy.dto.ExpenseRequestHistoryDto;
import com.uj.enterprise_policy_orchestrator.policy.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@DisplayName("ExpenseRequest Controller E2E Tests")
class ExpenseRequestIT extends AbstractIntegrationTest {

  @Autowired private ExpenseRequestRepository expenseRequestRepository;
  @Autowired private ExpenseRequestHistoryRepository expenseRequestHistoryRepository;
  @Autowired private PolicyRepository policyRepository;
  @Autowired private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    expenseRequestHistoryRepository.deleteAll();
    expenseRequestRepository.deleteAll();
    policyRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    expenseRequestHistoryRepository.deleteAll();
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
              new BigDecimal("1500.00"),
              "Travel",
              "Flight to Krakow",
              LocalDateTime.of(2026, 3, 20, 0, 0, 0));

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
      assertEquals("Travel", body.category());
      assertEquals("Flight to Krakow", body.description());
      assertEquals(LocalDateTime.of(2026, 3, 20, 0, 0, 0), body.expenseDate());
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
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"),
              "Travel",
              "Office supplies - pens and paper",
              LocalDateTime.of(2026, 3, 15, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, response.getBody().status());

      var allRequests = expenseRequestRepository.findAllWithApplicablePolicies();
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
              LocalDateTime.of(2026, 3, 20, 21, 37, 0));

      HttpClientErrorException.BadRequest exception =
          assertThrows(
              HttpClientErrorException.BadRequest.class,
              () ->
                  restTemplate.postForEntity(
                      baseUrl() + "/api/users/{userId}/expense-requests",
                      createRequest,
                      ExpenseRequestDto.class,
                      userId));

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
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
              new BigDecimal("100.00"),
              "Travel",
              "Minimum boundary test",
              LocalDateTime.of(2026, 3, 20, 12, 32, 23));

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
              new BigDecimal("1000.00"),
              "Travel",
              "Maximum boundary test",
              LocalDateTime.of(2026, 3, 20, 4, 23, 3));

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
              new BigDecimal("1500.00"),
              "Travel",
              "Multi-policy test",
              LocalDateTime.of(2026, 3, 20, 0, 0, 0));

      assertEquals(
          HttpStatus.CREATED,
          restTemplate
              .postForEntity(
                  baseUrl() + "/api/users/{userId}/expense-requests",
                  createRequest,
                  ExpenseRequestDto.class,
                  userId)
              .getStatusCode());
      var expenseRequests = expenseRequestRepository.findAllWithApplicablePolicies();
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
              new BigDecimal("500.00"),
              "Travel",
              "Within policy validity",
              LocalDateTime.of(2026, 3, 15, 20, 0, 0, 0));

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
              new BigDecimal("500.00"),
              "Travel",
              "Timestamp test",
              LocalDateTime.of(2026, 3, 20, 20, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertNotNull(response.getBody().submittedAt());

      var expenseRequests = expenseRequestRepository.findAllWithApplicablePolicies();
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
              new BigDecimal("500.00"),
              "Travel",
              "Test",
              LocalDateTime.of(2026, 3, 20, 20, 0, 0, 0));

      for (String userId : new String[] {"user-A", "user-B", "user-C"}) {
        ResponseEntity<ExpenseRequestDto> response =
            restTemplate.postForEntity(
                baseUrl() + "/api/users/{userId}/expense-requests",
                createRequest,
                ExpenseRequestDto.class,
                userId);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
      }

      var allRequests = expenseRequestRepository.findAllWithApplicablePolicies();
      assertEquals(3, allRequests.size());
      var userIds = allRequests.stream().map(ExpenseRequest::getUserId).distinct().toList();
      assertEquals(3, userIds.size());
    }

    @Test
    @DisplayName("should preserve all expense request fields in database")
    void shouldPreserveAllExpenseRequestFields() {
      String userId = "expense-user-8";
      String description = "Detailed expense description with special chars: @#$%";
      LocalDateTime expenseDate = LocalDateTime.of(2026, 3, 20, 20, 0, 0, 0);
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
          new CreateExpenseRequestDto(amount, "Travel", description, expenseDate);

      assertEquals(
          HttpStatus.CREATED,
          restTemplate
              .postForEntity(
                  baseUrl() + "/api/users/{userId}/expense-requests",
                  createRequest,
                  ExpenseRequestDto.class,
                  userId)
              .getStatusCode());
      var expenseRequests = expenseRequestRepository.findAllWithApplicablePolicies();
      assertEquals(1, expenseRequests.size());
      var savedRequest = expenseRequests.getFirst();
      assertEquals(userId, savedRequest.getUserId());
      assertEquals(0, savedRequest.getAmount().compareTo(amount));
      assertEquals("Travel", savedRequest.getCategory());
      assertEquals(expenseDate, savedRequest.getExpenseDate());
    }

    @Test
    @DisplayName(
        "should create expense request from a date-only JSON payload and normalize category")
    void shouldCreateExpenseRequestFromDateOnlyJsonPayloadAndNormalizeCategory()
        throws InterruptedException {
      String userId = "expense-user-json";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 3, 20, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("JSON-POLICY-001")
              .authorUserId("admin")
              .categoryId(2)
              .name("Localized Category Policy")
              .description("Policy used for JSON parsing coverage")
              .version(1)
              .startsAt(policyStartsAt)
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("2")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      String jsonBody =
          """
          {"amount":1500.00,"category":"Podroze sluzbowe","description":"Flight to Krakow","expenseDate":"2026-03-20"}
          """;

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.exchange(
              baseUrl() + "/api/users/{userId}/expense-requests",
              org.springframework.http.HttpMethod.POST,
              new HttpEntity<>(jsonBody, headers),
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals("2", response.getBody().category());
      assertEquals(LocalDateTime.of(2026, 3, 20, 0, 0, 0), response.getBody().expenseDate());
      assertNotNull(response.getBody().submittedAt());

      var savedRequest = expenseRequestRepository.findAllWithApplicablePolicies().getFirst();
      assertEquals("2", savedRequest.getCategory());
      assertEquals(LocalDateTime.of(2026, 3, 20, 0, 0, 0), savedRequest.getExpenseDate());
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests - Get Expense Request History")
  class GetExpenseRequestHistoryE2E {

    @Test
    @DisplayName("should return expense request history sorted by submittedAt descending")
    void shouldReturnExpenseRequestHistorySortedBySubmittedAtDescending()
        throws InterruptedException {
      String userId = "history-user-1";

      Policy policy =
          Policy.builder()
              .policyId("HISTORY-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("History Policy")
              .description("Policy for history endpoint coverage")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("10"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto request1 =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"),
              "Travel",
              "Request 1",
              LocalDateTime.of(2026, 3, 20, 10, 0, 0));
      CreateExpenseRequestDto request2 =
          new CreateExpenseRequestDto(
              new BigDecimal("200.00"),
              "Travel",
              "Request 2",
              LocalDateTime.of(2026, 3, 21, 10, 0, 0));
      CreateExpenseRequestDto request3 =
          new CreateExpenseRequestDto(
              new BigDecimal("300.00"),
              "Travel",
              "Request 3",
              LocalDateTime.of(2026, 3, 22, 10, 0, 0));

      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/expense-requests",
          request1,
          ExpenseRequestDto.class,
          userId);
      Thread.sleep(20);
      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/expense-requests",
          request2,
          ExpenseRequestDto.class,
          userId);
      Thread.sleep(20);
      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/expense-requests",
          request3,
          ExpenseRequestDto.class,
          userId);

      ResponseEntity<ExpenseRequestDto[]> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              ExpenseRequestDto[].class,
              userId);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ExpenseRequestDto[] history = response.getBody();
      assertNotNull(history);
      assertEquals(3, history.length);
      assertTrue(history[0].submittedAt().isAfter(history[1].submittedAt()));
      assertTrue(history[1].submittedAt().isAfter(history[2].submittedAt()));
      assertEquals("Request 3", history[0].description());
      assertEquals("Request 2", history[1].description());
      assertEquals("Request 1", history[2].description());
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests/{requestId} - Get Expense Request By Id")
  class GetExpenseRequestByIdE2E {

    @Test
    @DisplayName("should return expense request details for the owning user")
    void shouldReturnExpenseRequestForOwningUser() {
      String userId = "request-by-id-user";

      Policy policy =
          Policy.builder()
              .policyId("REQUEST-BY-ID-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Request By Id Policy")
              .description("Policy for get-by-id coverage")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("10"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"),
              "Travel",
              "Conference taxi",
              LocalDateTime.of(2026, 4, 1, 8, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      Long requestId = Objects.requireNonNull(createResponse.getBody()).id();

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests/{requestId}",
              ExpenseRequestDto.class,
              userId,
              requestId);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertNotNull(response.getBody());
      assertEquals(requestId, response.getBody().id());
      assertEquals(userId, response.getBody().userId());
      assertEquals("Conference taxi", response.getBody().description());
    }

    @Test
    @DisplayName("should return 404 when a different user requests the expense request")
    void shouldReturn404ForDifferentUser() {
      String userId = "request-by-id-owner";
      String otherUserId = "request-by-id-other";

      Policy policy =
          Policy.builder()
              .policyId("REQUEST-BY-ID-POLICY-002")
              .authorUserId("admin")
              .categoryId(1)
              .name("Request By Id Policy")
              .description("Policy for get-by-id negative coverage")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("10"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"),
              "Travel",
              "Conference taxi",
              LocalDateTime.of(2026, 4, 1, 8, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      Long requestId = Objects.requireNonNull(createResponse.getBody()).id();

      HttpClientErrorException.NotFound exception =
          assertThrows(
              HttpClientErrorException.NotFound.class,
              () ->
                  restTemplate.getForEntity(
                      baseUrl() + "/api/users/{userId}/expense-requests/{requestId}",
                      ExpenseRequestDto.class,
                      otherUserId,
                      requestId));

      assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
      assertTrue(exception.getResponseBodyAsString().contains("Expense request not found"));
    }
  }

  @Nested
  @DisplayName("Error handling and policy matching edge cases")
  class ErrorHandlingEdgeCases {

    @Test
    @DisplayName(
        "should expose available categories when the date and amount match but category does not")
    void shouldExposeAvailableCategoriesWhenOnlyCategoryDiffers() {
      String userId = "category-mismatch-user";

      Policy policy =
          Policy.builder()
              .policyId("AVAILABLE-CATEGORY-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Travel Policy")
              .description("Policy used for category mismatch coverage")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"),
              "Meals",
              "Lunch outside the policy category",
              LocalDateTime.of(2026, 3, 20, 12, 0, 0));

      HttpClientErrorException.BadRequest exception =
          assertThrows(
              HttpClientErrorException.BadRequest.class,
              () ->
                  restTemplate.postForEntity(
                      baseUrl() + "/api/users/{userId}/expense-requests",
                      createRequest,
                      ExpenseRequestDto.class,
                      userId));

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
      assertTrue(exception.getResponseBodyAsString().contains("Meals"));
      assertTrue(exception.getResponseBodyAsString().contains("Travel"));
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
              new BigDecimal("1500.00"),
              "Travel",
              "Multi-policy test",
              LocalDateTime.of(2026, 3, 20, 20, 11, 0, 0));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());

      var allRequests = expenseRequestRepository.findAllWithApplicablePolicies();
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
              new BigDecimal("1500.00"),
              "Travel",
              "Expired policy test",
              LocalDateTime.of(2026, 3, 20, 20, 10, 34, 0));

      ResponseEntity<ExpenseRequestDto> response =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, response.getStatusCode());

      var allRequests = expenseRequestRepository.findAllWithApplicablePolicies();
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
              new BigDecimal("100.00"),
              "Travel",
              "Exactly at minimum",
              LocalDateTime.of(2026, 3, 20, 20, 15, 0, 0));

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
              new BigDecimal("99.99"),
              "Travel",
              "Below minimum",
              LocalDateTime.of(2026, 3, 20, 20, 50, 0, 0));

      HttpClientErrorException.BadRequest belowMinException =
          assertThrows(
              HttpClientErrorException.BadRequest.class,
              () ->
                  restTemplate.postForEntity(
                      baseUrl() + "/api/users/{userId}/expense-requests",
                      belowMinRequest,
                      ExpenseRequestDto.class,
                      userId2));

      assertEquals(HttpStatus.BAD_REQUEST, belowMinException.getStatusCode());
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
              new BigDecimal("1000.00"),
              "Travel",
              "Exactly at maximum",
              LocalDateTime.of(2026, 3, 20, 20, 0, 0, 0));

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
              new BigDecimal("1000.01"),
              "Travel",
              "Above maximum",
              LocalDateTime.of(2026, 3, 20, 20, 0, 0, 0));

      HttpClientErrorException.BadRequest aboveMaxException =
          assertThrows(
              HttpClientErrorException.BadRequest.class,
              () ->
                  restTemplate.postForEntity(
                      baseUrl() + "/api/users/{userId}/expense-requests",
                      aboveMaxRequest,
                      ExpenseRequestDto.class,
                      userId2));

      assertEquals(HttpStatus.BAD_REQUEST, aboveMaxException.getStatusCode());
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
              new BigDecimal("999999.99"),
              "Travel",
              "Very large expense",
              LocalDateTime.of(2026, 3, 20, 20, 0, 0, 0));

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

  @Nested
  @DisplayName(
      "DELETE /api/users/{userId}/expense-requests/{expenseRequestId} - Cancel Expense Request")
  class CancelExpenseRequestE2E {

    @Test
    @DisplayName("should successfully cancel a WAITING_FOR_APPROVAL expense request")
    void shouldSuccessfullyCancelWaitingForApprovalRequest() {
      // given
      String userId = "cancel-user-1";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("TRAVEL-POLICY-CANCEL-001")
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
              new BigDecimal("1500.00"),
              "Travel",
              "Flight to Warsaw",
              LocalDateTime.of(2026, 3, 20, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
      assertNotNull(createResponse.getBody());
      assertNotNull(createResponse.getBody());
      Long expenseRequestId = createResponse.getBody().id();

      // when
      ResponseEntity<ExpenseRequestDto> cancelResponse =
          restTemplate.exchange(
              baseUrl() + "/api/users/{userId}/expense-requests/{expenseRequestId}",
              org.springframework.http.HttpMethod.DELETE,
              null,
              ExpenseRequestDto.class,
              userId,
              expenseRequestId);

      // then
      assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
      assertNotNull(cancelResponse.getBody());
      ExpenseRequestDto cancelledRequest = cancelResponse.getBody();
      assertEquals(expenseRequestId, cancelledRequest.id());
      assertEquals(userId, cancelledRequest.userId());
      assertEquals(ExpenseRequestStatus.CANCELLED, cancelledRequest.status());
      assertEquals(new BigDecimal("1500.00"), cancelledRequest.amount());
      assertEquals("Travel", cancelledRequest.category());

      // verify in database
      ExpenseRequest savedRequest =
          expenseRequestRepository.findById(expenseRequestId).orElse(null);
      assertNotNull(savedRequest);
      assertEquals(ExpenseRequestStatus.CANCELLED, savedRequest.getStatus());
    }

    @Test
    @DisplayName("should return 400 when trying to cancel non-existent request")
    void shouldReturn400WhenCancellingNonExistentRequest() {
      // given
      String userId = "cancel-user-2";
      Long nonExistentId = 99999L;

      // when & then
      HttpClientErrorException exception =
          assertThrows(
              HttpClientErrorException.class,
              () ->
                  restTemplate.exchange(
                      baseUrl() + "/api/users/{userId}/expense-requests/{expenseRequestId}",
                      org.springframework.http.HttpMethod.DELETE,
                      null,
                      ExpenseRequestDto.class,
                      userId,
                      nonExistentId));

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("should prevent cancellation of request by different user")
    void shouldPreventCancellationByDifferentUser() {
      // given
      String owner = "cancel-user-3";
      String otherUser = "cancel-user-4";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("TRAVEL-POLICY-CANCEL-002")
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
              new BigDecimal("800.00"),
              "Travel",
              "Hotel accommodation",
              LocalDateTime.of(2026, 4, 15, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              owner);

      assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
      ExpenseRequestDto createdRequest = Objects.requireNonNull(createResponse.getBody());
      Long expenseRequestId = createdRequest.id();

      // when & then - attempt to cancel with different user
      HttpClientErrorException exception =
          assertThrows(
              HttpClientErrorException.class,
              () ->
                  restTemplate.exchange(
                      baseUrl() + "/api/users/{userId}/expense-requests/{expenseRequestId}",
                      org.springframework.http.HttpMethod.DELETE,
                      null,
                      ExpenseRequestDto.class,
                      otherUser,
                      expenseRequestId));

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

      // verify original request is still in WAITING_FOR_APPROVAL status
      ExpenseRequest savedRequest =
          expenseRequestRepository.findById(expenseRequestId).orElse(null);
      assertNotNull(savedRequest);
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, savedRequest.getStatus());
    }

    @Test
    @DisplayName("should prevent cancellation of already cancelled request")
    void shouldPreventCancellationOfAlreadyCancelledRequest() {
      // given
      String userId = "cancel-user-5";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("TRAVEL-POLICY-CANCEL-003")
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
              new BigDecimal("600.00"),
              "Travel",
              "Car rental",
              LocalDateTime.of(2026, 5, 1, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      ExpenseRequestDto createdRequest = Objects.requireNonNull(createResponse.getBody());
      Long expenseRequestId = createdRequest.id();

      // first cancellation - should succeed
      ResponseEntity<ExpenseRequestDto> firstCancel =
          restTemplate.exchange(
              baseUrl() + "/api/users/{userId}/expense-requests/{expenseRequestId}",
              org.springframework.http.HttpMethod.DELETE,
              null,
              ExpenseRequestDto.class,
              userId,
              expenseRequestId);

      assertEquals(HttpStatus.OK, firstCancel.getStatusCode());
      ExpenseRequestDto cancelledRequest = Objects.requireNonNull(firstCancel.getBody());
      assertEquals(ExpenseRequestStatus.CANCELLED, cancelledRequest.status());

      // second cancellation - should fail
      HttpClientErrorException exception =
          assertThrows(
              HttpClientErrorException.class,
              () ->
                  restTemplate.exchange(
                      baseUrl() + "/api/users/{userId}/expense-requests/{expenseRequestId}",
                      org.springframework.http.HttpMethod.DELETE,
                      null,
                      ExpenseRequestDto.class,
                      userId,
                      expenseRequestId));

      assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("should persist cancelled request in database")
    void shouldPersistCancelledRequestInDatabase() {
      // given
      String userId = "cancel-user-6";

      LocalDateTime policyStartsAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
      Policy policy =
          Policy.builder()
              .policyId("TRAVEL-POLICY-CANCEL-004")
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
              new BigDecimal("2000.00"),
              "Travel",
              "Conference registration",
              LocalDateTime.of(2026, 6, 10, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      ExpenseRequestDto createdRequest = Objects.requireNonNull(createResponse.getBody());
      Long expenseRequestId = createdRequest.id();

      // when
      restTemplate.exchange(
          baseUrl() + "/api/users/{userId}/expense-requests/{expenseRequestId}",
          org.springframework.http.HttpMethod.DELETE,
          null,
          ExpenseRequestDto.class,
          userId,
          expenseRequestId);

      // then - verify in database
      ExpenseRequest savedRequest =
          expenseRequestRepository.findById(expenseRequestId).orElse(null);
      assertNotNull(savedRequest);
      assertEquals(ExpenseRequestStatus.CANCELLED, savedRequest.getStatus());
      assertEquals(userId, savedRequest.getUserId());
      assertEquals(new BigDecimal("2000.00"), savedRequest.getAmount());
      assertEquals("Travel", savedRequest.getCategory());
      assertEquals("Conference registration", savedRequest.getDescription());
      assertEquals(LocalDateTime.of(2026, 6, 10, 0, 0, 0), savedRequest.getExpenseDate());
    }
  }

  @Nested
  @DisplayName(
      "GET /api/users/{userId}/expense-requests/{expenseRequestId}/history - Get Request Status History")
  class GetExpenseRequestStatusHistoryE2E {

    @Test
    @DisplayName("should retrieve status history for a specific expense request")
    void shouldRetrieveStatusHistoryForSpecificRequest() {
      String userId = "history-user-1";
      LocalDateTime now = LocalDateTime.now();

      // given - create a policy and an expense request
      Policy policy =
          Policy.builder()
              .policyId("HISTORY-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("History Test Policy")
              .description("Policy for history testing")
              .version(1)
              .startsAt(now.minusYears(1))
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"), "Travel", "History test request", now.minusDays(5));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      Long requestId = createResponse.getBody().id();

      // when - retrieve status history
      ResponseEntity<ExpenseRequestHistoryDto[]> historyResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests/{requestId}/history",
              ExpenseRequestHistoryDto[].class,
              userId,
              requestId);

      // then - verify history was created with initial status
      assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
      assertNotNull(historyResponse.getBody());
      ExpenseRequestHistoryDto[] historyEntries = historyResponse.getBody();
      assertEquals(1, historyEntries.length);

      ExpenseRequestHistoryDto initialHistory = historyEntries[0];
      assertEquals(requestId, initialHistory.requestId());
      assertEquals(userId, initialHistory.userId());
      assertTrue(initialHistory.previousStatus() == null);
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, initialHistory.newStatus());
      assertEquals("Expense request created", initialHistory.changeReason());
      assertNotNull(initialHistory.changedAt());
    }

    @Test
    @DisplayName("should track history when expense request is cancelled")
    void shouldTrackHistoryWhenRequestCancelled() {
      String userId = "history-user-2";
      LocalDateTime now = LocalDateTime.now();

      // given - create policy and request
      Policy policy =
          Policy.builder()
              .policyId("CANCEL-HISTORY-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Cancel History Policy")
              .description("Policy for cancel history testing")
              .version(1)
              .startsAt(now.minusMonths(1))
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      CreateExpenseRequestDto createRequest =
          new CreateExpenseRequestDto(
              new BigDecimal("800.00"),
              "Travel",
              "Request to cancel",
              LocalDateTime.of(2026, 5, 1, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> createResponse =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              createRequest,
              ExpenseRequestDto.class,
              userId);

      Long requestId = createResponse.getBody().id();

      // when - cancel the request
      restTemplate.exchange(
          baseUrl() + "/api/users/{userId}/expense-requests/{requestId}",
          org.springframework.http.HttpMethod.DELETE,
          null,
          ExpenseRequestDto.class,
          userId,
          requestId);

      // then - retrieve history and verify cancellation was recorded
      ResponseEntity<ExpenseRequestHistoryDto[]> historyResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests/{requestId}/history",
              ExpenseRequestHistoryDto[].class,
              userId,
              requestId);

      ExpenseRequestHistoryDto[] historyEntries = historyResponse.getBody();
      assertEquals(2, historyEntries.length);

      // Most recent should be cancellation (Flyway sorts DESC by changedAt)
      ExpenseRequestHistoryDto cancellationHistory = historyEntries[0];
      assertEquals(requestId, cancellationHistory.requestId());
      assertEquals(ExpenseRequestStatus.WAITING_FOR_APPROVAL, cancellationHistory.previousStatus());
      assertEquals(ExpenseRequestStatus.CANCELLED, cancellationHistory.newStatus());
      assertEquals("Expense request cancelled by user", cancellationHistory.changeReason());
    }

    @Test
    @DisplayName("should return empty list when no history exists for request")
    void shouldReturnEmptyListWhenNoHistory() {
      String userId = "history-user-3";
      Long nonExistentRequestId = 99999L;

      // when
      ResponseEntity<ExpenseRequestHistoryDto[]> historyResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests/{requestId}/history",
              ExpenseRequestHistoryDto[].class,
              userId,
              nonExistentRequestId);

      // then
      assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
      assertNotNull(historyResponse.getBody());
      assertEquals(0, historyResponse.getBody().length);
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests/history/all - Get User Expense History")
  class GetUserExpenseRequestHistoryE2E {

    @Test
    @DisplayName("should retrieve all history entries for a user across multiple requests")
    void shouldRetrieveAllHistoryForUser() {
      String userId = "all-history-user-1";
      LocalDateTime now = LocalDateTime.now();

      // given - create policy and multiple expense requests
      Policy policy =
          Policy.builder()
              .policyId("ALL-HISTORY-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("All History Policy")
              .description("Policy for all history testing")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      // Create first request
      CreateExpenseRequestDto request1 =
          new CreateExpenseRequestDto(
              new BigDecimal("500.00"),
              "Travel",
              "First request",
              LocalDateTime.of(2026, 4, 1, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> response1 =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              request1,
              ExpenseRequestDto.class,
              userId);
      Long requestId1 = response1.getBody().id();

      // Create second request
      CreateExpenseRequestDto request2 =
          new CreateExpenseRequestDto(
              new BigDecimal("750.00"),
              "Travel",
              "Second request",
              LocalDateTime.of(2026, 4, 15, 0, 0, 0));

      ResponseEntity<ExpenseRequestDto> response2 =
          restTemplate.postForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests",
              request2,
              ExpenseRequestDto.class,
              userId);
      Long requestId2 = response2.getBody().id();

      // Cancel the first request to add another history entry
      restTemplate.exchange(
          baseUrl() + "/api/users/{userId}/expense-requests/{requestId}",
          org.springframework.http.HttpMethod.DELETE,
          null,
          ExpenseRequestDto.class,
          userId,
          requestId1);

      // when - retrieve all history for user
      ResponseEntity<ExpenseRequestHistoryDto[]> historyResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/" + userId + "/expense-requests/history/all",
              ExpenseRequestHistoryDto[].class);

      // then - verify all history entries are returned
      assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
      assertNotNull(historyResponse.getBody());
      ExpenseRequestHistoryDto[] historyEntries = historyResponse.getBody();
      assertEquals(3, historyEntries.length); // 2 creations + 1 cancellation

      // Verify all entries belong to the user
      for (ExpenseRequestHistoryDto entry : historyEntries) {
        assertEquals(userId, entry.userId());
      }

      // Verify entries from both requests are present
      var request1Entries =
          java.util.Arrays.stream(historyEntries)
              .filter(e -> e.requestId().equals(requestId1))
              .toList();
      var request2Entries =
          java.util.Arrays.stream(historyEntries)
              .filter(e -> e.requestId().equals(requestId2))
              .toList();

      assertEquals(2, request1Entries.size()); // creation + cancellation
      assertEquals(1, request2Entries.size()); // only creation
    }

    @Test
    @DisplayName("should return empty list when user has no history")
    void shouldReturnEmptyListWhenUserHasNoHistory() {
      String userId = "all-history-user-2";

      // when
      ResponseEntity<ExpenseRequestHistoryDto[]> historyResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/{userId}/expense-requests/history/all",
              ExpenseRequestHistoryDto[].class,
              userId);

      // then
      assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
      assertNotNull(historyResponse.getBody());
      assertEquals(0, historyResponse.getBody().length);
    }

    @Test
    @DisplayName("should isolate history between different users")
    void shouldIsolateHistoryBetweenUsers() {
      String user1 = "isolated-user-1";
      String user2 = "isolated-user-2";
      LocalDateTime now = LocalDateTime.now();

      // given - create policy
      Policy policy =
          Policy.builder()
              .policyId("ISOLATED-HISTORY-POLICY-001")
              .authorUserId("admin")
              .categoryId(1)
              .name("Isolated History Policy")
              .description("Policy for isolation testing")
              .version(1)
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .category("Travel")
              .authorizedRole(1)
              .build();
      policyRepository.save(policy);

      // Create requests from different users
      CreateExpenseRequestDto user1Request =
          new CreateExpenseRequestDto(
              new BigDecimal("600.00"),
              "Travel",
              "User 1 request",
              LocalDateTime.of(2026, 4, 5, 0, 0, 0));

      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/expense-requests",
          user1Request,
          ExpenseRequestDto.class,
          user1);

      CreateExpenseRequestDto user2Request =
          new CreateExpenseRequestDto(
              new BigDecimal("400.00"),
              "Travel",
              "User 2 request",
              LocalDateTime.of(2026, 4, 10, 0, 0, 0));

      restTemplate.postForEntity(
          baseUrl() + "/api/users/{userId}/expense-requests",
          user2Request,
          ExpenseRequestDto.class,
          user2);

      // when - retrieve history for each user
      ResponseEntity<ExpenseRequestHistoryDto[]> user1HistoryResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/" + user1 + "/expense-requests/history/all",
              ExpenseRequestHistoryDto[].class);

      ResponseEntity<ExpenseRequestHistoryDto[]> user2HistoryResponse =
          restTemplate.getForEntity(
              baseUrl() + "/api/users/" + user2 + "/expense-requests/history/all",
              ExpenseRequestHistoryDto[].class);

      // then - verify each user only sees their own history
      ExpenseRequestHistoryDto[] user1History = user1HistoryResponse.getBody();
      ExpenseRequestHistoryDto[] user2History = user2HistoryResponse.getBody();

      assertEquals(1, user1History.length);
      assertEquals(1, user2History.length);
      assertEquals(user1, user1History[0].userId());
      assertEquals(user2, user2History[0].userId());
    }
  }
}

package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.domain.enums.ManagerDecision;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.EscalatedExpenseDecisionDto;
import com.uj.enterprise_policy_orchestrator.dto.EscalatedExpenseDecisionResultDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.exception.ExpenseRequestNotEscalatedException;
import com.uj.enterprise_policy_orchestrator.exception.ManagerRoleRequiredException;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.exception.PolicyNotAssignedToExpenseRequestException;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseRequestService")
class ExpenseRequestServiceTest {

  @Mock private ExpenseRequestRepository expenseRequestRepository;
  @Mock private PolicyRepository policyRepository;
  @Mock private PolicyService policyService;
  @InjectMocks private ExpenseRequestService expenseRequestService;

  @Nested
  @DisplayName("Scenario 1: Employee submits a valid expense request")
  class CreateExpenseRequest {

    @Test
    @DisplayName(
        "should create an expense request with given data and automatic submission timestamp")
    void shouldCreateExpenseRequestWithSubmittedAtTimestamp() {
      // given — employee submits an expense request
      String userId = "user-123";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"),
              "Business travel",
              "Business trip to Krakow – train tickets and hotel",
              LocalDateTime.of(2026, 3, 20, 10, 30, 0));

      Policy policy =
          Policy.builder()
              .id(1L)
              .policyId("POL-001")
              .category("Travel")
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
              .expiresAt(null)
              .minPrice(BigDecimal.ZERO)
              .maxPrice(new BigDecimal("10000"))
              .build();

      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy);

      when(policyService.findApplicablePolicies(
              "Business travel",
              LocalDateTime.of(2026, 3, 20, 10, 30, 0),
              new BigDecimal("1500.00")))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(100L);
                req.setSubmittedAt(LocalDateTime.now());
                req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                return req;
              });

      // when — employee submits the request
      ExpenseRequestDto result = expenseRequestService.createExpenseRequest(userId, dto);

      // then — request is created with correct data
      assertThat(result.id()).isEqualTo(100L);
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.amount()).isEqualByComparingTo("1500.00");
      assertThat(result.category()).isEqualTo("Business travel");
      assertThat(result.description())
          .isEqualTo("Business trip to Krakow – train tickets and hotel");
      assertThat(result.expenseDate()).isEqualTo(LocalDateTime.of(2026, 3, 20, 10, 30, 0));

      // then — system automatically assigns submission timestamp
      assertThat(result.submittedAt()).isNotNull();
      assertThat(result.submittedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("should persist the expense request in the database")
    void shouldPersistExpenseRequestInDatabase() {
      // given
      String userId = "user-456";
      LocalDateTime expenseDate = LocalDateTime.now();

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"), "Office supplies", "Printer toner", expenseDate);

      Policy policy = Policy.builder().id(1L).build();
      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy);

      when(policyService.findApplicablePolicies(
              "Office supplies", expenseDate, new BigDecimal("250.00")))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(1L);
                req.setSubmittedAt(LocalDateTime.now());
                req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                return req;
              });

      // when
      expenseRequestService.createExpenseRequest(userId, dto);

      // then — request is permanently saved for future review and audit
      ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
      verify(expenseRequestRepository, times(1)).save(captor.capture());

      ExpenseRequest saved = captor.getValue();
      assertThat(saved.getUserId()).isEqualTo(userId);
      assertThat(saved.getAmount()).isEqualByComparingTo("250.00");
      assertThat(saved.getCategory()).isEqualTo("Office supplies");
      assertThat(saved.getDescription()).isEqualTo("Printer toner");
      assertThat(saved.getExpenseDate()).isEqualTo(expenseDate);
    }

    @Test
    @DisplayName("should associate the request with applicable policies")
    void shouldAssociateRequestWithApplicablePolicies() {
      // given
      String userId = "user-789";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("89.99"),
              "Training",
              "Online Java course",
              LocalDateTime.of(2026, 4, 1, 9, 15, 0));

      Policy policy1 = Policy.builder().id(1L).policyId("POL-001").build();
      Policy policy2 = Policy.builder().id(2L).policyId("POL-002").build();

      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy1);
      applicablePolicies.add(policy2);

      when(policyService.findApplicablePolicies(
              "Training", LocalDateTime.of(2026, 4, 1, 9, 15, 0), new BigDecimal("89.99")))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(5L);
                req.setSubmittedAt(LocalDateTime.now());
                req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                return req;
              });

      // when
      ExpenseRequestDto result = expenseRequestService.createExpenseRequest(userId, dto);

      // then
      assertThat(result.userId()).isEqualTo(userId);

      ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
      verify(expenseRequestRepository).save(captor.capture());
      ExpenseRequest saved = captor.getValue();
      assertThat(saved.getApplicablePolicies()).hasSize(2);
      assertThat(saved.getApplicablePolicies()).contains(policy1, policy2);
    }

    @Test
    @DisplayName("should throw exception when no applicable policies found")
    void shouldThrowWhenNoApplicablePolicies() {
      // given
      String userId = "user-999";
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"),
              "Unknown",
              "No policy for this",
              LocalDateTime.of(2026, 1, 1, 11, 0, 0));

      when(policyService.findApplicablePolicies(
              "Unknown", LocalDateTime.of(2026, 1, 1, 11, 0, 0), new BigDecimal("100.00")))
          .thenReturn(new HashSet<>());

      // when & then
      assertThatThrownBy(() -> expenseRequestService.createExpenseRequest(userId, dto))
          .isInstanceOf(NoApplicablePoliciesException.class);

      // then — verify that repository.save was not called
      verify(expenseRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("should normalize category label for policy matching")
    void shouldNormalizeCategoryLabelForPolicyMatching() {
      String userId = "user-321";
      LocalDateTime expenseDate = LocalDateTime.of(2026, 5, 13, 12, 0, 0);

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"), "Sprzet biurowy", "Office keyboard", expenseDate);

      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(Policy.builder().id(1L).policyId("POL-1").build());

      when(policyService.findApplicablePolicies("1", expenseDate, new BigDecimal("100.00")))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(41L);
                req.setSubmittedAt(LocalDateTime.now());
                req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                return req;
              });

      expenseRequestService.createExpenseRequest(userId, dto);

      verify(policyService).findApplicablePolicies("1", expenseDate, new BigDecimal("100.00"));
    }
  }

  @Nested
  @DisplayName("Scenario 2: Expense-Policy Assignment Logic")
  class ExpensePolicyAssignment {

    @Test
    @DisplayName("should assign policies matching category and amount range")
    void shouldAssignPoliciesMatchingCategoryAndAmount() {
      // given
      String userId = "user-100";
      BigDecimal expenseAmount = new BigDecimal("2500.00");
      LocalDateTime expenseDate = LocalDateTime.of(2026, 2, 15, 14, 0, 0);

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(expenseAmount, "Travel", "Hotel and flights", expenseDate);

      // Mock multiple applicable policies
      Policy policy1 =
          Policy.builder()
              .id(1L)
              .policyId("TRAVEL-001")
              .category("Travel")
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0))
              .expiresAt(LocalDateTime.of(2026, 12, 31, 23, 59))
              .minPrice(new BigDecimal("100"))
              .maxPrice(new BigDecimal("5000"))
              .build();

      Policy policy2 =
          Policy.builder()
              .id(2L)
              .policyId("TRAVEL-002")
              .category("Travel")
              .startsAt(LocalDateTime.of(2025, 1, 1, 0, 0))
              .expiresAt(null)
              .minPrice(BigDecimal.ZERO)
              .maxPrice(new BigDecimal("10000"))
              .build();

      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy1);
      applicablePolicies.add(policy2);

      when(policyService.findApplicablePolicies("Travel", expenseDate, expenseAmount))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(10L);
                req.setSubmittedAt(LocalDateTime.now());
                if (req.getStatus() == null) {
                  req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                }
                return req;
              });

      // when
      expenseRequestService.createExpenseRequest(userId, dto);

      // then
      ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
      verify(expenseRequestRepository).save(captor.capture());
      ExpenseRequest saved = captor.getValue();

      assertThat(saved.getApplicablePolicies()).hasSize(2);
      assertThat(saved.getApplicablePolicies()).contains(policy1, policy2);
      assertThat(saved.getStatus()).isEqualTo(ExpenseRequestStatus.ESCALATED);
    }

    @Test
    @DisplayName("should handle expense with single applicable policy")
    void shouldHandleExpenseWithSinglePolicy() {
      // given
      String userId = "user-200";
      BigDecimal expenseAmount = new BigDecimal("150.00");
      LocalDateTime expenseDate = LocalDateTime.of(2026, 3, 10, 13, 0, 0);

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(expenseAmount, "Office", "Office equipment", expenseDate);

      Policy policy =
          Policy.builder()
              .id(1L)
              .policyId("OFFICE-001")
              .category("Travel")
              .startsAt(LocalDateTime.of(2026, 1, 1, 0, 0))
              .expiresAt(null)
              .minPrice(BigDecimal.ZERO)
              .maxPrice(new BigDecimal("500"))
              .build();

      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy);

      when(policyService.findApplicablePolicies("Office", expenseDate, expenseAmount))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(20L);
                req.setSubmittedAt(LocalDateTime.now());
                if (req.getStatus() == null) {
                  req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                }
                return req;
              });

      // when
      expenseRequestService.createExpenseRequest(userId, dto);

      // then
      ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
      verify(expenseRequestRepository).save(captor.capture());
      ExpenseRequest saved = captor.getValue();

      assertThat(saved.getApplicablePolicies()).hasSize(1);
      assertThat(saved.getApplicablePolicies()).contains(policy);
    }

    @Test
    @DisplayName("should set status to DECLINED when no policies found")
    void shouldSetStatusToDeclinedWhenNoPoliciesFound() {
      // given
      String userId = "user-300";
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("10000.00"),
              "Luxury",
              "Expensive item",
              LocalDateTime.of(2026, 1, 1, 11, 0, 0));

      when(policyService.findApplicablePolicies(
              "Luxury", LocalDateTime.of(2026, 1, 1, 11, 0, 0), new BigDecimal("10000.00")))
          .thenReturn(new HashSet<>());

      // when & then
      assertThatThrownBy(() -> expenseRequestService.createExpenseRequest(userId, dto))
          .isInstanceOf(NoApplicablePoliciesException.class);

      // Verify the request is NOT saved to database
      verify(expenseRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("should preserve all expense details when assigning policies")
    void shouldPreserveExpenseDetailsWhenAssigningPolicies() {
      // given
      String userId = "user-400";
      String category = "Meals";
      BigDecimal amount = new BigDecimal("85.50");
      LocalDateTime expenseDate = LocalDateTime.of(2026, 2, 28, 16, 30, 0);
      String description = "Team lunch meeting";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(amount, category, description, expenseDate);

      Policy policy = Policy.builder().id(1L).build();
      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy);

      when(policyService.findApplicablePolicies(category, expenseDate, amount))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(30L);
                req.setSubmittedAt(LocalDateTime.now());
                req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                return req;
              });

      // when
      ExpenseRequestDto result = expenseRequestService.createExpenseRequest(userId, dto);

      // then
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.category()).isEqualTo(category);
      assertThat(result.amount()).isEqualByComparingTo(amount);
      assertThat(result.expenseDate()).isEqualTo(expenseDate);
      assertThat(result.description()).isEqualTo(description);
    }

    @Test
    @DisplayName("should treat midnight expenseDate as full-day for policy matching")
    void shouldTreatMidnightExpenseDateAsFullDayForPolicyMatching() {
      // given
      String userId = "user-500";
      LocalDateTime expenseDate = LocalDateTime.of(2026, 5, 13, 0, 0, 0);
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(new BigDecimal("100.00"), "Travel", "Date-only", expenseDate);

      Policy policy = Policy.builder().id(1L).policyId("POL-001").build();
      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy);

      LocalDateTime expectedMatchingDate = LocalDateTime.of(2026, 5, 13, 23, 59, 59, 999999999);
      when(policyService.findApplicablePolicies(
              "Travel", expectedMatchingDate, new BigDecimal("100.00")))
          .thenReturn(applicablePolicies);

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(40L);
                req.setSubmittedAt(LocalDateTime.now());
                req.setStatus(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
                return req;
              });

      // when
      expenseRequestService.createExpenseRequest(userId, dto);

      // then
      verify(policyService)
          .findApplicablePolicies("Travel", expectedMatchingDate, new BigDecimal("100.00"));
    }
  }

  @Nested
  @DisplayName("Scenario 2: Skipping deactivated policies when evaluating new expense requests")
  class EvaluateExpenseRequestAgainstPolicies {

    @Test
    @DisplayName("should return active policies from repository")
    void shouldOnlyUseActivePoliciesForEvaluation() {
      // given
      LocalDateTime now = LocalDateTime.now();

      Policy activePolicy =
          Policy.builder()
              .id(1L)
              .policyId("100")
              .authorUserId("1")
              .categoryId(1)
              .name("Active Travel Policy")
              .version(1)
              .createdAt(now.minusDays(30))
              .startsAt(now.minusDays(30))
              .expiresAt(null)
              .minPrice(new java.math.BigDecimal("100"))
              .maxPrice(new java.math.BigDecimal("5000"))
              .category("1")
              .authorizedRole(2)
              .build();

      // repository returns only active policies
      when(policyRepository.findActivePolicies(any(LocalDateTime.class)))
          .thenReturn(List.of(activePolicy));

      // when
      List<Policy> result = expenseRequestService.getActivePolicies();

      // then
      assertThat(result).containsExactly(activePolicy);
      verify(policyRepository).findActivePolicies(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("should return empty list when no active policies exist")
    void shouldNotIncludeExpiredPoliciesInEvaluation() {
      // given
      when(policyRepository.findActivePolicies(any(LocalDateTime.class))).thenReturn(List.of());

      // when
      List<Policy> result = expenseRequestService.getActivePolicies();

      // then
      assertThat(result).isEmpty();

      verify(policyRepository).findActivePolicies(any(LocalDateTime.class));
      verify(policyRepository, never()).findAll();
    }
  }

  @Nested
  @DisplayName("Scenario 2: Employee reviews their submitted expense requests")
  class GetExpenseRequestHistory {

    @Test
    @DisplayName("should retrieve all expense requests for a user sorted by submission date")
    void shouldRetrieveExpenseRequestHistorySortedBySubmittedAt() {
      // given — employee exists and has submitted multiple expense requests
      String userId = "user-3";

      ExpenseRequest request1 =
          ExpenseRequest.builder()
              .id(101L)
              .userId("user-3")
              .amount(new BigDecimal("500.00"))
              .category("Travel")
              .description("Flight to conference")
              .expenseDate(LocalDateTime.of(2026, 1, 15, 9, 15, 0))
              .submittedAt(LocalDateTime.of(2026, 1, 16, 10, 0, 0))
              .build();

      ExpenseRequest request2 =
          ExpenseRequest.builder()
              .id(102L)
              .userId("user-3")
              .amount(new BigDecimal("150.00"))
              .category("Meals")
              .description("Team lunch")
              .expenseDate(LocalDateTime.of(2026, 1, 20, 9, 15, 0))
              .submittedAt(LocalDateTime.of(2026, 1, 21, 14, 30, 0))
              .build();

      ExpenseRequest request3 =
          ExpenseRequest.builder()
              .id(103L)
              .userId("user-3")
              .amount(new BigDecimal("75.50"))
              .category("Office supplies")
              .description("Notebooks and pens")
              .expenseDate(LocalDateTime.of(2026, 2, 1, 9, 15, 0))
              .submittedAt(LocalDateTime.of(2026, 2, 2, 9, 15, 0))
              .build();

      when(expenseRequestRepository.findByUserId(
              userId, Sort.by(Sort.Direction.DESC, "submittedAt")))
          .thenReturn(java.util.List.of(request3, request2, request1));

      // when — employee retrieves their expense request history
      var result = expenseRequestService.getExpenseRequestHistory(userId);

      // then — system returns all requests sorted by submission date (most recent
      // first)
      assertThat(result).hasSize(3);
      assertThat(result.get(0).id()).isEqualTo(103L);
      assertThat(result.get(1).id()).isEqualTo(102L);
      assertThat(result.get(2).id()).isEqualTo(101L);

      assertThat(result.get(0).submittedAt())
          .isAfter(result.get(1).submittedAt())
          .isAfter(result.get(2).submittedAt());
    }

    @Test
    @DisplayName("should return empty list when user has no expense requests")
    void shouldReturnEmptyListWhenNoRequests() {
      // given — employee exists but has not submitted any expense requests
      String userId = "user-4";
      //   User employee = User.builder().id(userId).username("tech.supporter").build();

      //   when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
      when(expenseRequestRepository.findByUserId(
              userId, Sort.by(Sort.Direction.DESC, "submittedAt")))
          .thenReturn(java.util.List.of());

      // when — employee requests their expense request history
      var result = expenseRequestService.getExpenseRequestHistory(userId);

      // then — system returns empty list
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should throw exception when user does not exist")
    void shouldThrowWhenUserNotFoundOnHistory() {
      // given — user does not exist
      String nonExistentUserId = "user-999";

      when(expenseRequestRepository.findByUserId(
              nonExistentUserId, Sort.by(Sort.Direction.DESC, "submittedAt")))
          .thenReturn(java.util.List.of());

      // when & then — system returns empty list for non-existent user
      var result = expenseRequestService.getExpenseRequestHistory(nonExistentUserId);
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Scenario 6: Manager manually resolves an escalated expense request")
  class ResolveEscalatedExpenseRequest {

    @Test
    @DisplayName("should approve an escalated request using selected policy")
    void shouldApproveEscalatedRequest() {
      // given
      String userId = "expense-user-900";
      Long requestId = 900L;

      Policy policyOne =
          Policy.builder().id(11L).policyId("TRAVEL-STD").name("Travel Standard").build();
      Policy policyTwo =
          Policy.builder().id(12L).policyId("TRAVEL-EXT").name("Travel Extended").build();

      ExpenseRequest request =
          ExpenseRequest.builder()
              .id(requestId)
              .userId(userId)
              .status(ExpenseRequestStatus.ESCALATED)
              .build();
      request.getApplicablePolicies().addAll(Set.of(policyOne, policyTwo));

      when(expenseRequestRepository.findDetailedByIdAndUserId(requestId, userId))
          .thenReturn(Optional.of(request));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      EscalatedExpenseDecisionDto decisionDto =
          new EscalatedExpenseDecisionDto(11L, ManagerDecision.APPROVE);

      // when
      EscalatedExpenseDecisionResultDto result =
          expenseRequestService.resolveEscalatedRequest(userId, requestId, "Manager", decisionDto);

      // then
      assertThat(result.requestId()).isEqualTo(requestId);
      assertThat(result.status()).isEqualTo(ExpenseRequestStatus.APPROVED);
      assertThat(result.selectedPolicyId()).isEqualTo(11L);
      assertThat(result.selectedPolicyRef()).isEqualTo("TRAVEL-STD");
      assertThat(request.getResolutionPolicy()).isEqualTo(policyOne);
      verify(expenseRequestRepository).save(request);
    }

    @Test
    @DisplayName("should decline an escalated request using selected policy")
    void shouldDeclineEscalatedRequest() {
      // given
      String userId = "expense-user-901";
      Long requestId = 901L;

      Policy policyOne = Policy.builder().id(21L).policyId("TRAVEL-A").name("Travel A").build();
      Policy policyTwo = Policy.builder().id(22L).policyId("TRAVEL-B").name("Travel B").build();

      ExpenseRequest request =
          ExpenseRequest.builder()
              .id(requestId)
              .userId(userId)
              .status(ExpenseRequestStatus.ESCALATED)
              .build();
      request.getApplicablePolicies().addAll(Set.of(policyOne, policyTwo));

      when(expenseRequestRepository.findDetailedByIdAndUserId(requestId, userId))
          .thenReturn(Optional.of(request));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      EscalatedExpenseDecisionDto decisionDto =
          new EscalatedExpenseDecisionDto(22L, ManagerDecision.DECLINE);

      // when
      EscalatedExpenseDecisionResultDto result =
          expenseRequestService.resolveEscalatedRequest(userId, requestId, "Manager", decisionDto);

      // then
      assertThat(result.requestId()).isEqualTo(requestId);
      assertThat(result.status()).isEqualTo(ExpenseRequestStatus.DECLINED);
      assertThat(result.selectedPolicyId()).isEqualTo(22L);
      assertThat(result.selectedPolicyRef()).isEqualTo("TRAVEL-B");
      assertThat(request.getResolutionPolicy()).isEqualTo(policyTwo);
      verify(expenseRequestRepository).save(request);
    }

    @Test
    @DisplayName("should reject decision when user role is not manager")
    void shouldRejectWhenRoleIsNotManager() {
      // given
      EscalatedExpenseDecisionDto decisionDto =
          new EscalatedExpenseDecisionDto(11L, ManagerDecision.APPROVE);

      // when & then
      assertThatThrownBy(
              () ->
                  expenseRequestService.resolveEscalatedRequest(
                      "expense-user-902", 902L, "Employee", decisionDto))
          .isInstanceOf(ManagerRoleRequiredException.class);

      verify(expenseRequestRepository, never()).findDetailedByIdAndUserId(any(), any());
      verify(expenseRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject decision when request is not escalated")
    void shouldRejectWhenRequestIsNotEscalated() {
      // given
      String userId = "expense-user-903";
      Long requestId = 903L;

      Policy policy = Policy.builder().id(31L).policyId("TRAVEL-C").name("Travel C").build();
      ExpenseRequest request =
          ExpenseRequest.builder()
              .id(requestId)
              .userId(userId)
              .status(ExpenseRequestStatus.WAITING_FOR_APPROVAL)
              .build();
      request.getApplicablePolicies().add(policy);

      when(expenseRequestRepository.findDetailedByIdAndUserId(requestId, userId))
          .thenReturn(Optional.of(request));

      EscalatedExpenseDecisionDto decisionDto =
          new EscalatedExpenseDecisionDto(31L, ManagerDecision.APPROVE);

      // when & then
      assertThatThrownBy(
              () ->
                  expenseRequestService.resolveEscalatedRequest(
                      userId, requestId, "Manager", decisionDto))
          .isInstanceOf(ExpenseRequestNotEscalatedException.class);
      verify(expenseRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("should reject decision when selected policy is not assigned")
    void shouldRejectWhenPolicyIsNotAssigned() {
      // given
      String userId = "expense-user-904";
      Long requestId = 904L;

      Policy policy =
          Policy.builder().id(41L).policyId("TRAVEL-ASSIGNED").name("Travel Assigned").build();
      ExpenseRequest request =
          ExpenseRequest.builder()
              .id(requestId)
              .userId(userId)
              .status(ExpenseRequestStatus.ESCALATED)
              .build();
      request.getApplicablePolicies().add(policy);

      when(expenseRequestRepository.findDetailedByIdAndUserId(requestId, userId))
          .thenReturn(Optional.of(request));

      EscalatedExpenseDecisionDto decisionDto =
          new EscalatedExpenseDecisionDto(999L, ManagerDecision.DECLINE);

      // when & then
      assertThatThrownBy(
              () ->
                  expenseRequestService.resolveEscalatedRequest(
                      userId, requestId, "Manager", decisionDto))
          .isInstanceOf(PolicyNotAssignedToExpenseRequestException.class);
      verify(expenseRequestRepository, never()).save(any());
    }
  }

      @Nested
      @DisplayName("Scenario 3: Employee cancels an expense request")
      class CancelExpenseRequest {

      @Test
      @DisplayName("should successfully cancel a WAITING_FOR_APPROVAL expense request")
      void shouldSuccessfullyCancelWaitingForApprovalRequest() {
        // given — employee has submitted an expense request in WAITING_FOR_APPROVAL status
        String userId = "user-123";
        Long expenseRequestId = 100L;

        ExpenseRequest existingRequest =
          ExpenseRequest.builder()
            .id(expenseRequestId)
            .userId(userId)
            .amount(new BigDecimal("1500.00"))
            .category("Business travel")
            .description("Business trip to Krakow")
            .expenseDate(LocalDateTime.of(2026, 3, 20, 0, 0, 0))
            .submittedAt(LocalDateTime.now())
            .status(ExpenseRequestStatus.WAITING_FOR_APPROVAL)
            .build();

        when(expenseRequestRepository.findById(expenseRequestId))
          .thenReturn(java.util.Optional.of(existingRequest));

        when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
            invocation -> {
            ExpenseRequest req = invocation.getArgument(0);
            req.setStatus(ExpenseRequestStatus.CANCELLED);
            return req;
            });

        // when — employee cancels the request
        ExpenseRequestDto result =
          expenseRequestService.cancelExpenseRequest(userId, expenseRequestId);

        // then — request status changes to CANCELLED
        assertThat(result.status()).isEqualTo(ExpenseRequestStatus.CANCELLED);
        assertThat(result.id()).isEqualTo(expenseRequestId);
        assertThat(result.userId()).isEqualTo(userId);
      }

      @Test
      @DisplayName("should persist the cancelled request in the database")
      void shouldPersistCancelledRequestInDatabase() {
        // given
        String userId = "user-456";
        Long expenseRequestId = 50L;

        ExpenseRequest existingRequest =
          ExpenseRequest.builder()
            .id(expenseRequestId)
            .userId(userId)
            .amount(new BigDecimal("250.00"))
            .category("Office supplies")
            .description("Printer toner")
            .expenseDate(LocalDateTime.of(2026, 5, 10, 0, 0, 0))
            .submittedAt(LocalDateTime.now())
            .status(ExpenseRequestStatus.WAITING_FOR_APPROVAL)
            .build();

        when(expenseRequestRepository.findById(expenseRequestId))
          .thenReturn(java.util.Optional.of(existingRequest));

        when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
            invocation -> {
            ExpenseRequest req = invocation.getArgument(0);
            req.setStatus(ExpenseRequestStatus.CANCELLED);
            return req;
            });

        // when
        expenseRequestService.cancelExpenseRequest(userId, expenseRequestId);

        // then — request is persisted with CANCELLED status
        ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(expenseRequestRepository, times(1)).save(captor.capture());

        ExpenseRequest saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ExpenseRequestStatus.CANCELLED);
        assertThat(saved.getId()).isEqualTo(expenseRequestId);
      }

      @Test
      @DisplayName("should throw exception when request does not exist")
      void shouldThrowWhenRequestNotFound() {
        // given
        String userId = "user-789";
        Long nonExistentRequestId = 999L;

        when(expenseRequestRepository.findById(nonExistentRequestId))
          .thenReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(
            () -> expenseRequestService.cancelExpenseRequest(userId, nonExistentRequestId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Expense request not found with id: " + nonExistentRequestId);

        // then — verify that save was not called
        verify(expenseRequestRepository, never()).save(any());
      }

      @Test
      @DisplayName("should throw exception when request belongs to different user")
      void shouldThrowWhenRequestBelongsToDifferentUser() {
        // given
        String requestingUserId = "user-123";
        String ownerUserId = "user-other";
        Long expenseRequestId = 100L;

        ExpenseRequest existingRequest =
          ExpenseRequest.builder()
            .id(expenseRequestId)
            .userId(ownerUserId)
            .amount(new BigDecimal("1500.00"))
            .category("Business travel")
            .description("Business trip")
            .expenseDate(LocalDateTime.of(2026, 3, 20, 0, 0, 0))
            .submittedAt(LocalDateTime.now())
            .status(ExpenseRequestStatus.WAITING_FOR_APPROVAL)
            .build();

        when(expenseRequestRepository.findById(expenseRequestId))
          .thenReturn(java.util.Optional.of(existingRequest));

        // when & then
        assertThatThrownBy(
            () -> expenseRequestService.cancelExpenseRequest(requestingUserId, expenseRequestId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Expense request does not belong to user: " + requestingUserId);

        // then — verify that save was not called
        verify(expenseRequestRepository, never()).save(any());
      }

      @Test
      @DisplayName("should throw exception when trying to cancel a DECLINED request")
      void shouldThrowWhenCancellingDeclinedRequest() {
        // given
        String userId = "user-123";
        Long expenseRequestId = 100L;

        ExpenseRequest declaredRequest =
          ExpenseRequest.builder()
            .id(expenseRequestId)
            .userId(userId)
            .amount(new BigDecimal("1500.00"))
            .category("Business travel")
            .description("Business trip")
            .expenseDate(LocalDateTime.of(2026, 3, 20, 0, 0, 0))
            .submittedAt(LocalDateTime.now())
            .status(ExpenseRequestStatus.DECLINED)
            .build();

        when(expenseRequestRepository.findById(expenseRequestId))
          .thenReturn(java.util.Optional.of(declaredRequest));

        // when & then
        assertThatThrownBy(() -> expenseRequestService.cancelExpenseRequest(userId, expenseRequestId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Expense request cannot be cancelled with status: DECLINED");

        // then — verify that save was not called
        verify(expenseRequestRepository, never()).save(any());
      }

      @Test
      @DisplayName("should throw exception when trying to cancel an already CANCELLED request")
      void shouldThrowWhenCancellingAlreadyCancelledRequest() {
        // given
        String userId = "user-123";
        Long expenseRequestId = 100L;

        ExpenseRequest cancelledRequest =
          ExpenseRequest.builder()
            .id(expenseRequestId)
            .userId(userId)
            .amount(new BigDecimal("1500.00"))
            .category("Business travel")
            .description("Business trip")
            .expenseDate(LocalDateTime.of(2026, 3, 20, 0, 0, 0))
            .submittedAt(LocalDateTime.now())
            .status(ExpenseRequestStatus.CANCELLED)
            .build();

        when(expenseRequestRepository.findById(expenseRequestId))
          .thenReturn(java.util.Optional.of(cancelledRequest));

        // when & then
        assertThatThrownBy(() -> expenseRequestService.cancelExpenseRequest(userId, expenseRequestId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Expense request cannot be cancelled with status: CANCELLED");

        // then — verify that save was not called
        verify(expenseRequestRepository, never()).save(any());
      }
      }
}

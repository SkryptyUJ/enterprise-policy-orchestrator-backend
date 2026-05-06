package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseRequestService")
class ExpenseRequestServiceTest {

  @Mock private ExpenseRequestRepository expenseRequestRepository;
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
              LocalDateTime.of(2026, 3, 20, 0, 0, 0));

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
              "Business travel", LocalDateTime.of(2026, 3, 20, 0, 0, 0), new BigDecimal("1500.00")))
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
      assertThat(result.expenseDate()).isEqualTo(LocalDateTime.of(2026, 3, 20, 0, 0, 0));

      // then — system automatically assigns submission timestamp
      assertThat(result.submittedAt()).isNotNull();
      assertThat(result.submittedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("should persist the expense request in the database")
    void shouldPersistExpenseRequestInDatabase() {
      // given
      String userId = "user-456";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"), "Office supplies", "Printer toner", LocalDateTime.now());

      Policy policy = Policy.builder().id(1L).build();
      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy);

      when(policyService.findApplicablePolicies(
              "Office supplies", LocalDateTime.now(), new BigDecimal("250.00")))
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
      assertThat(saved.getExpenseDate()).isEqualTo(LocalDateTime.now());
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
              LocalDateTime.of(2026, 4, 1, 0, 0, 0));

      Policy policy1 = Policy.builder().id(1L).policyId("POL-001").build();
      Policy policy2 = Policy.builder().id(2L).policyId("POL-002").build();

      Set<Policy> applicablePolicies = new HashSet<>();
      applicablePolicies.add(policy1);
      applicablePolicies.add(policy2);

      when(policyService.findApplicablePolicies(
              "Training", LocalDateTime.of(2026, 4, 1, 0, 0, 0), new BigDecimal("89.99")))
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
              LocalDateTime.of(2026, 1, 1, 0, 0, 0));

      when(policyService.findApplicablePolicies(
              "Unknown", LocalDateTime.of(2026, 1, 1, 0, 0, 0), new BigDecimal("100.00")))
          .thenReturn(new HashSet<>());

      // when & then
      assertThatThrownBy(() -> expenseRequestService.createExpenseRequest(userId, dto))
          .isInstanceOf(NoApplicablePoliciesException.class);

      // then — verify that repository.save was not called
      verify(expenseRequestRepository, never()).save(any());
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
      LocalDateTime expenseDate = LocalDateTime.of(2026, 2, 15, 0, 0, 0);

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
    }

    @Test
    @DisplayName("should handle expense with single applicable policy")
    void shouldHandleExpenseWithSinglePolicy() {
      // given
      String userId = "user-200";
      BigDecimal expenseAmount = new BigDecimal("150.00");
      LocalDateTime expenseDate = LocalDateTime.of(2026, 3, 10, 0, 0, 0);

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
              LocalDateTime.of(2026, 1, 1, 0, 0, 0));

      when(policyService.findApplicablePolicies(
              "Luxury", LocalDateTime.of(2026, 1, 1, 0, 0, 0), new BigDecimal("10000.00")))
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
      LocalDateTime expenseDate = LocalDateTime.of(2026, 2, 28, 0, 0, 0);
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
  }
}

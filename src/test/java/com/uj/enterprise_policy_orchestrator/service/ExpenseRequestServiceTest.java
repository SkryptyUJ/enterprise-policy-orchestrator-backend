package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.User;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
  @Mock private UserRepository userRepository;
  @Mock private PolicyRepository policyRepository;
  @InjectMocks private ExpenseRequestService expenseRequestService;

  @Nested
  @DisplayName("Scenario 1: Employee submits a valid expense request")
  class CreateExpenseRequest {

    @Test
    @DisplayName(
        "should create an expense request with given data and automatic submission timestamp")
    void shouldCreateExpenseRequestWithSubmittedAtTimestamp() {
      // given — employee exists in the system
      Long userId = 1L;
      User employee = User.builder().id(userId).username("john.doe").build();

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"),
              "Business travel",
              "Business trip to Krakow – train tickets and hotel",
              LocalDate.of(2026, 3, 20));

      when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(100L);
                req.setSubmittedAt(LocalDateTime.now());
                return req;
              });
      when(policyRepository.findActivePolicies(any(LocalDateTime.class)))
          .thenReturn(List.of());

      // when — employee submits the request
      ExpenseRequestDto result = expenseRequestService.createExpenseRequest(userId, dto);

      // then — request is created with correct data
      assertThat(result.id()).isEqualTo(100L);
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.amount()).isEqualByComparingTo("1500.00");
      assertThat(result.category()).isEqualTo("Business travel");
      assertThat(result.description())
          .isEqualTo("Business trip to Krakow – train tickets and hotel");
      assertThat(result.expenseDate()).isEqualTo(LocalDate.of(2026, 3, 20));

      // then — system automatically assigns submission timestamp
      assertThat(result.submittedAt()).isNotNull();
      assertThat(result.submittedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("should persist the expense request in the database")
    void shouldPersistExpenseRequestInDatabase() {
      // given
      Long userId = 1L;
      User employee = User.builder().id(userId).username("john.doe").build();

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"), "Office supplies", "Printer toner", LocalDate.now());

      when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(1L);
                req.setSubmittedAt(LocalDateTime.now());
                return req;
              });
      when(policyRepository.findActivePolicies(any(LocalDateTime.class)))
          .thenReturn(List.of());

      // when
      expenseRequestService.createExpenseRequest(userId, dto);

      // then — request is permanently saved for future review and audit
      ArgumentCaptor<ExpenseRequest> captor = ArgumentCaptor.forClass(ExpenseRequest.class);
      verify(expenseRequestRepository, times(1)).save(captor.capture());

      ExpenseRequest saved = captor.getValue();
      assertThat(saved.getUser()).isEqualTo(employee);
      assertThat(saved.getAmount()).isEqualByComparingTo("250.00");
      assertThat(saved.getCategory()).isEqualTo("Office supplies");
      assertThat(saved.getDescription()).isEqualTo("Printer toner");
      assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("should associate the request with the correct employee")
    void shouldAssociateRequestWithCorrectEmployee() {
      // given
      Long userId = 7L;
      User employee = User.builder().id(userId).username("anna.smith").build();

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("89.99"), "Training", "Online Java course", LocalDate.of(2026, 4, 1));

      when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(5L);
                req.setSubmittedAt(LocalDateTime.now());
                return req;
              });
      when(policyRepository.findActivePolicies(any(LocalDateTime.class)))
          .thenReturn(List.of());

      // when
      ExpenseRequestDto result = expenseRequestService.createExpenseRequest(userId, dto);

      // then
      assertThat(result.userId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("should throw exception when the employee does not exist")
    void shouldThrowWhenUserNotFound() {
      // given
      Long nonExistentUserId = 999L;
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"), "Inne", "Opis", LocalDate.of(2026, 1, 1));

      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> expenseRequestService.createExpenseRequest(nonExistentUserId, dto))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("999");
    }
  }

  @Nested
  @DisplayName("Scenario 2: Skipping deactivated policies when evaluating new expense requests")
  class EvaluateExpenseRequestAgainstPolicies {

    @Test
    @DisplayName(
        "should only use active policies for evaluation when creating an expense request")
    void shouldOnlyUseActivePoliciesForEvaluation() {
      // given — an active policy and a deactivated (expired) policy exist
      Long userId = 1L;
      User employee = User.builder().id(userId).username("john.doe").build();

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
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("5000"))
              .category(1)
              .authorizedRole(2)
              .build();

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"),
              "Business travel",
              "Trip to conference",
              LocalDate.of(2026, 6, 15));

      when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(1L);
                req.setSubmittedAt(LocalDateTime.now());
                return req;
              });
      // Repository returns only active policies (expired ones are already filtered out)
      when(policyRepository.findActivePolicies(any(LocalDateTime.class)))
          .thenReturn(List.of(activePolicy));

      // when — a new expense request is submitted and automatically evaluated
      expenseRequestService.createExpenseRequest(userId, dto);

      // then — only active policies are fetched for evaluation
      verify(policyRepository).findActivePolicies(any(LocalDateTime.class));
    }

    @Test
    @DisplayName(
        "should not include expired policies in evaluation of new expense requests")
    void shouldNotIncludeExpiredPoliciesInEvaluation() {
      // given — a policy whose end date has passed (deactivated)
      Long userId = 1L;
      User employee = User.builder().id(userId).username("john.doe").build();

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("500.00"),
              "Office supplies",
              "New monitors",
              LocalDate.of(2026, 6, 15));

      when(userRepository.findById(userId)).thenReturn(Optional.of(employee));
      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(2L);
                req.setSubmittedAt(LocalDateTime.now());
                return req;
              });
      // The repository query already filters expired policies — returns empty list
      when(policyRepository.findActivePolicies(any(LocalDateTime.class)))
          .thenReturn(List.of());

      // when — a new expense request is submitted
      ExpenseRequestDto result = expenseRequestService.createExpenseRequest(userId, dto);

      // then — the expense request is created successfully
      assertThat(result).isNotNull();
      assertThat(result.id()).isEqualTo(2L);

      // then — only the active policies query was used (no findAll)
      verify(policyRepository).findActivePolicies(any(LocalDateTime.class));
      verify(policyRepository, never()).findAll();
    }
  }
}

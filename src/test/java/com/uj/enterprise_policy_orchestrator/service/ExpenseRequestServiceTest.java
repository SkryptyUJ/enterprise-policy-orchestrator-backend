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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
      // given
      String userId = "1";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"),
              "Business travel",
              "Business trip to Krakow - train tickets and hotel",
              LocalDate.of(2026, 3, 20));

      when(policyService.findApplicablePolicies(any(), any(), any()))
          .thenReturn(Set.of(new Policy()));

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(100L);
                req.setSubmittedAt(LocalDateTime.now());
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
          .isEqualTo("Business trip to Krakow - train tickets and hotel");
      assertThat(result.expenseDate()).isEqualTo(LocalDate.of(2026, 3, 20));
      assertThat(result.status()).isEqualTo(ExpenseRequestStatus.WAITING_FOR_APPROVAL);

      // then — system automatically assigns submission timestamp
      assertThat(result.submittedAt()).isNotNull();
      assertThat(result.submittedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("should persist the expense request in the database")
    void shouldPersistExpenseRequestInDatabase() {
      // given
      String userId = "1";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("250.00"), "Office supplies", "Printer toner", LocalDate.now());

      when(policyService.findApplicablePolicies(any(), any(), any()))
          .thenReturn(Set.of(new Policy()));

      when(expenseRequestRepository.save(any(ExpenseRequest.class)))
          .thenAnswer(
              invocation -> {
                ExpenseRequest req = invocation.getArgument(0);
                req.setId(1L);
                req.setSubmittedAt(LocalDateTime.now());
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
      assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.now());
      assertThat(saved.getStatus()).isEqualTo(ExpenseRequestStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    @DisplayName("should decline request when no applicable policies are found")
    void shouldDeclineWhenNoApplicablePoliciesFound() {
      // given
      String userId = "2";

      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("99.99"), "Misc", "Snacks", LocalDate.of(2026, 7, 1));

      when(policyService.findApplicablePolicies(any(), any(), any())).thenReturn(Set.of());

      // when / then
      assertThatThrownBy(() -> expenseRequestService.createExpenseRequest(userId, dto))
          .isInstanceOf(NoApplicablePoliciesException.class)
          .hasMessage("Decline, no matching policies");

      verify(expenseRequestRepository, never()).save(any());
    }
  }
}

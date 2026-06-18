package com.uj.enterprise_policy_orchestrator.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uj.enterprise_policy_orchestrator.expense_request.controller.ExpenseRequestController;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ApproveExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.expense_request.service.ExpenseRequestService;
import com.uj.enterprise_policy_orchestrator.policy.dto.ExpenseRequestHistoryDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseRequestController")
class ExpenseRequestControllerTest {

  @Mock private ExpenseRequestService expenseRequestService;

  @InjectMocks private ExpenseRequestController expenseRequestController;

  @Nested
  @DisplayName("createExpenseRequest")
  class CreateExpenseRequestTests {

    @Test
    @DisplayName("should delegate with JWT subject")
    void shouldDelegateWithJwtSubject() {
      String userId = "user-123";
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("1500.00"), 1, "Business trip", LocalDateTime.of(2026, 3, 20, 0, 0));

      ExpenseRequestDto expected =
          sampleExpenseRequest(100L, userId, ExpenseRequestStatus.WAITING_FOR_APPROVAL);

      when(expenseRequestService.createExpenseRequest(userId, Set.of(1), dto)).thenReturn(expected);

      ExpenseRequestDto result = expenseRequestController.createExpenseRequest(jwtFor(userId), dto);

      assertEquals(expected, result);
      verify(expenseRequestService).createExpenseRequest(userId, Set.of(1), dto);
    }

    @Test
    @DisplayName("should return 401 when JWT is missing")
    void shouldReturn401WhenJwtMissing() {
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"), 1, "Description", LocalDateTime.of(2026, 5, 13, 0, 0));

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> expenseRequestController.createExpenseRequest(null, dto));

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    @DisplayName("should return 401 when JWT subject is blank")
    void shouldReturn401WhenJwtSubjectBlank() {
      CreateExpenseRequestDto dto =
          new CreateExpenseRequestDto(
              new BigDecimal("100.00"), 1, "Description", LocalDateTime.of(2026, 5, 13, 0, 0));

      Jwt jwtWithoutSubject =
          Jwt.withTokenValue("token-no-sub").header("alg", "none").claim("sub", " ").build();

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> expenseRequestController.createExpenseRequest(jwtWithoutSubject, dto));

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
  }

  @Nested
  @DisplayName("getExpenseRequestHistory")
  class GetExpenseRequestHistoryTests {

    @Test
    @DisplayName("should delegate with JWT subject")
    void shouldDelegateWithJwtSubject() {
      String userId = "history-user-1";
      List<ExpenseRequestDto> expected =
          List.of(sampleExpenseRequest(1L, userId, ExpenseRequestStatus.WAITING_FOR_APPROVAL));

      when(expenseRequestService.getExpenseRequestHistory(userId)).thenReturn(expected);

      List<ExpenseRequestDto> result =
          expenseRequestController.getExpenseRequestHistory(jwtFor(userId));

      assertEquals(expected, result);
      verify(expenseRequestService).getExpenseRequestHistory(userId);
    }
  }

  @Nested
  @DisplayName("review endpoints")
  class ReviewEndpointTests {

    @Test
    @DisplayName("should delegate review list")
    void shouldDelegateReviewList() {
      List<ExpenseRequestDto> expected =
          List.of(
              sampleExpenseRequest(10L, "employee-1", ExpenseRequestStatus.WAITING_FOR_APPROVAL));

      when(expenseRequestService.getExpenseRequestHistoryForReview()).thenReturn(expected);

      List<ExpenseRequestDto> result =
          expenseRequestController.getExpenseRequestHistoryForReview(jwtFor("reviewer-1"));

      assertEquals(expected, result);
      verify(expenseRequestService).getExpenseRequestHistoryForReview();
    }

    @Test
    @DisplayName("should delegate review details by id")
    void shouldDelegateReviewDetailsById() {
      Long requestId = 55L;
      ExpenseRequestDto expected =
          sampleExpenseRequest(requestId, "employee-2", ExpenseRequestStatus.WAITING_FOR_APPROVAL);

      when(expenseRequestService.getExpenseRequestByIdForReview(requestId)).thenReturn(expected);

      ExpenseRequestDto result =
          expenseRequestController.getExpenseRequestByIdForReview(jwtFor("reviewer-2"), requestId);

      assertEquals(expected, result);
      verify(expenseRequestService).getExpenseRequestByIdForReview(requestId);
    }

    @Test
    @DisplayName("should delegate approve with reviewer id from JWT")
    void shouldDelegateApproveWithReviewerIdFromJwt() {
      String reviewerId = "manager-1";
      Long requestId = 71L;
      ApproveExpenseRequestDto dto = new ApproveExpenseRequestDto("policy compliant");
      ExpenseRequestDto expected =
          sampleExpenseRequest(requestId, "employee-3", ExpenseRequestStatus.APPROVED);

      when(expenseRequestService.approveExpenseRequest(reviewerId, requestId, "policy compliant"))
          .thenReturn(expected);

      ExpenseRequestDto result =
          expenseRequestController.approveExpenseRequest(jwtFor(reviewerId), requestId, dto);

      assertEquals(expected, result);
      verify(expenseRequestService)
          .approveExpenseRequest(reviewerId, requestId, "policy compliant");
    }

    @Test
    @DisplayName("should delegate decline with reviewer id from JWT")
    void shouldDelegateDeclineWithReviewerIdFromJwt() {
      String reviewerId = "manager-2";
      Long requestId = 72L;
      ApproveExpenseRequestDto dto = new ApproveExpenseRequestDto("outside policy");
      ExpenseRequestDto expected =
          sampleExpenseRequest(requestId, "employee-4", ExpenseRequestStatus.DECLINED);

      when(expenseRequestService.declineExpenseRequest(reviewerId, requestId, "outside policy"))
          .thenReturn(expected);

      ExpenseRequestDto result =
          expenseRequestController.declineExpenseRequest(jwtFor(reviewerId), requestId, dto);

      assertEquals(expected, result);
      verify(expenseRequestService).declineExpenseRequest(reviewerId, requestId, "outside policy");
    }

    @Test
    @DisplayName("should return 401 for review list when JWT is missing")
    void shouldReturn401ForReviewListWhenJwtMissing() {
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> expenseRequestController.getExpenseRequestHistoryForReview(null));

      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
  }

  @Nested
  @DisplayName("other endpoints")
  class OtherEndpointTests {

    @Test
    @DisplayName("should delegate cancelExpenseRequest")
    void shouldDelegateCancelExpenseRequest() {
      String userId = "user-456";
      Long requestId = 42L;
      ExpenseRequestDto expected =
          sampleExpenseRequest(requestId, userId, ExpenseRequestStatus.CANCELLED);

      when(expenseRequestService.cancelExpenseRequest(userId, requestId)).thenReturn(expected);

      ExpenseRequestDto result =
          expenseRequestController.cancelExpenseRequest(jwtFor(userId), requestId);

      assertEquals(expected, result);
      verify(expenseRequestService).cancelExpenseRequest(userId, requestId);
    }

    @Test
    @DisplayName("should delegate getUserExpenseRequestHistory")
    void shouldDelegateGetUserExpenseRequestHistory() {
      String userId = "user-history-1";
      List<ExpenseRequestHistoryDto> expected =
          List.of(sampleHistory(1L, 100L, userId, null, ExpenseRequestStatus.WAITING_FOR_APPROVAL));

      when(expenseRequestService.getUserExpenseRequestHistory(userId)).thenReturn(expected);

      List<ExpenseRequestHistoryDto> result =
          expenseRequestController.getUserExpenseRequestHistory(jwtFor(userId));

      assertEquals(expected, result);
      verify(expenseRequestService).getUserExpenseRequestHistory(userId);
    }

    @Test
    @DisplayName("should delegate getExpenseRequestById")
    void shouldDelegateGetExpenseRequestById() {
      String userId = "user-details-1";
      Long requestId = 7L;
      ExpenseRequestDto expected =
          sampleExpenseRequest(requestId, userId, ExpenseRequestStatus.WAITING_FOR_APPROVAL);

      when(expenseRequestService.getExpenseRequestById(userId, requestId)).thenReturn(expected);

      ExpenseRequestDto result =
          expenseRequestController.getExpenseRequestById(jwtFor(userId), requestId);

      assertEquals(expected, result);
      verify(expenseRequestService).getExpenseRequestById(userId, requestId);
    }

    @Test
    @DisplayName("should delegate getExpenseRequestStatusHistory")
    void shouldDelegateGetExpenseRequestStatusHistory() {
      String userId = "user-history-2";
      Long requestId = 8L;
      List<ExpenseRequestHistoryDto> expected =
          List.of(
              sampleHistory(
                  2L,
                  requestId,
                  userId,
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL,
                  ExpenseRequestStatus.CANCELLED));

      when(expenseRequestService.getExpenseRequestStatusHistory(userId, requestId))
          .thenReturn(expected);

      List<ExpenseRequestHistoryDto> result =
          expenseRequestController.getExpenseRequestStatusHistory(jwtFor(userId), requestId);

      assertEquals(expected, result);
      verify(expenseRequestService).getExpenseRequestStatusHistory(userId, requestId);
    }
  }

  private static Jwt jwtFor(String subject) {
    return Jwt.withTokenValue("token-" + subject)
        .header("alg", "none")
        .claim("sub", subject)
        .claim("roles", List.of("employee"))
        .build();
  }

  private static ExpenseRequestDto sampleExpenseRequest(
      Long id, String userId, ExpenseRequestStatus status) {
    return new ExpenseRequestDto(
        id,
        userId,
        new BigDecimal("100.00"),
        1,
        "Sprzet biurowy",
        "Sample description",
        LocalDateTime.of(2026, 5, 13, 0, 0),
        LocalDateTime.of(2026, 5, 14, 10, 0),
        status,
        null,
        null,
        null,
        null,
        null);
  }

  private static ExpenseRequestHistoryDto sampleHistory(
      Long id,
      Long requestId,
      String userId,
      ExpenseRequestStatus previousStatus,
      ExpenseRequestStatus newStatus) {
    return new ExpenseRequestHistoryDto(
        id,
        requestId,
        userId,
        previousStatus,
        newStatus,
        LocalDateTime.of(2026, 5, 14, 10, 0),
        "Sample change");
  }
}

package com.uj.enterprise_policy_orchestrator.expense_request.controller;

import com.uj.enterprise_policy_orchestrator.config.JwtRoleExtractor;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ApproveExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.service.ExpenseRequestService;
import com.uj.enterprise_policy_orchestrator.policy.dto.ExpenseRequestHistoryDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/expense-requests")
@RequiredArgsConstructor
public class ExpenseRequestController {

  private final ExpenseRequestService expenseRequestService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExpenseRequestDto createExpenseRequest(
      @AuthenticationPrincipal Jwt jwt, @RequestBody CreateExpenseRequestDto dto) {
    return expenseRequestService.createExpenseRequest(
        getAuthenticatedUserId(jwt), JwtRoleExtractor.extractPolicyRoleIds(jwt), dto);
  }

  @GetMapping
  public List<ExpenseRequestDto> getExpenseRequestHistory(@AuthenticationPrincipal Jwt jwt) {
    return expenseRequestService.getExpenseRequestHistory(getAuthenticatedUserId(jwt));
  }

  @GetMapping("/review")
  public List<ExpenseRequestDto> getExpenseRequestHistoryForReview(
      @AuthenticationPrincipal Jwt jwt) {
    getAuthenticatedUserId(jwt);
    return expenseRequestService.getExpenseRequestHistoryForReview();
  }

  @DeleteMapping("/{expenseRequestId}")
  @ResponseStatus(HttpStatus.OK)
  public ExpenseRequestDto cancelExpenseRequest(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("expenseRequestId") Long expenseRequestId) {
    return expenseRequestService.cancelExpenseRequest(
        getAuthenticatedUserId(jwt), expenseRequestId);
  }

  @GetMapping("/history/all")
  public List<ExpenseRequestHistoryDto> getUserExpenseRequestHistory(
      @AuthenticationPrincipal Jwt jwt) {
    return expenseRequestService.getUserExpenseRequestHistory(getAuthenticatedUserId(jwt));
  }

  @GetMapping("/{requestId}")
  public ExpenseRequestDto getExpenseRequestById(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("requestId") Long requestId) {
    return expenseRequestService.getExpenseRequestById(getAuthenticatedUserId(jwt), requestId);
  }

  @GetMapping("/{expenseRequestId}/history")
  public List<ExpenseRequestHistoryDto> getExpenseRequestStatusHistory(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("expenseRequestId") Long expenseRequestId) {
    return expenseRequestService.getExpenseRequestStatusHistory(
        getAuthenticatedUserId(jwt), expenseRequestId);
  }

  private String getAuthenticatedUserId(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing subject claim in JWT");
    }

    return jwt.getSubject();
  }

  @GetMapping("/review/{requestId}")
  public ExpenseRequestDto getExpenseRequestByIdForReview(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
    getAuthenticatedUserId(jwt);
    return expenseRequestService.getExpenseRequestByIdForReview(requestId);
  }

  @PatchMapping("/review/{requestId}/approve")
  @ResponseStatus(HttpStatus.OK)
  public ExpenseRequestDto approveExpenseRequest(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long requestId,
      @RequestBody ApproveExpenseRequestDto dto) {
    return expenseRequestService.approveExpenseRequest(
        getAuthenticatedUserId(jwt), requestId, dto.decisionRationale());
  }

  @PatchMapping("/review/{requestId}/decline")
  @ResponseStatus(HttpStatus.OK)
  public ExpenseRequestDto declineExpenseRequest(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long requestId,
      @RequestBody ApproveExpenseRequestDto dto) {
    return expenseRequestService.declineExpenseRequest(
        getAuthenticatedUserId(jwt), requestId, dto.decisionRationale());
  }
}

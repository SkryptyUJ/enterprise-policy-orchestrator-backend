package com.uj.enterprise_policy_orchestrator.controller;

import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.EscalatedExpenseDecisionDto;
import com.uj.enterprise_policy_orchestrator.dto.EscalatedExpenseDecisionResultDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDetailsDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.service.ExpenseRequestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/expense-requests")
@RequiredArgsConstructor
public class ExpenseRequestController {

  private final ExpenseRequestService expenseRequestService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ExpenseRequestDto createExpenseRequest(
      @PathVariable String userId, @RequestBody CreateExpenseRequestDto dto) {
    return expenseRequestService.createExpenseRequest(userId, dto);
  }

  @GetMapping
  public List<ExpenseRequestDto> getExpenseRequestHistory(@PathVariable Long userId) {
    return expenseRequestService.getExpenseRequestHistory(userId);
  }

  @GetMapping("/{requestId}")
  public ExpenseRequestDetailsDto getExpenseRequest(
      @PathVariable String userId, @PathVariable Long requestId) {
    return expenseRequestService.getExpenseRequestDetails(userId, requestId);
  }

  @PostMapping("/{requestId}/manager-decision")
  public EscalatedExpenseDecisionResultDto resolveEscalatedExpenseRequest(
      @PathVariable String userId,
      @PathVariable Long requestId,
      @RequestHeader(value = "X-User-Role", required = false) String userRole,
      @RequestBody EscalatedExpenseDecisionDto decisionDto) {
    return expenseRequestService.resolveEscalatedRequest(userId, requestId, userRole, decisionDto);
  }
}

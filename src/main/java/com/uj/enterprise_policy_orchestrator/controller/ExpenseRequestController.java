package com.uj.enterprise_policy_orchestrator.controller;

import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestHistoryDto;
import com.uj.enterprise_policy_orchestrator.service.ExpenseRequestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  public List<ExpenseRequestDto> getExpenseRequestHistory(@PathVariable String userId) {
    return expenseRequestService.getExpenseRequestHistory(userId);
  }

  @DeleteMapping("/{expenseRequestId}")
  @ResponseStatus(HttpStatus.OK)
  public ExpenseRequestDto cancelExpenseRequest(
      @PathVariable String userId, @PathVariable Long expenseRequestId) {
    return expenseRequestService.cancelExpenseRequest(userId, expenseRequestId);
  }

  @GetMapping("/{requestId}")
  public ExpenseRequestDto getExpenseRequestById(
      @PathVariable String userId, @PathVariable Long requestId) {
    return expenseRequestService.getExpenseRequestById(userId, requestId);
  }

  @GetMapping("/{expenseRequestId}/history")
  public List<ExpenseRequestHistoryDto> getExpenseRequestStatusHistory(
      @PathVariable String userId, @PathVariable Long expenseRequestId) {
    return expenseRequestService.getExpenseRequestStatusHistory(expenseRequestId);
  }

  @GetMapping("/history/all")
  public List<ExpenseRequestHistoryDto> getUserExpenseRequestHistory(@PathVariable String userId) {
    return expenseRequestService.getUserExpenseRequestHistory(userId);
  }
}

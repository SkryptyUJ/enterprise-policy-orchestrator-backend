package com.uj.enterprise_policy_orchestrator.controller;

import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.service.ExpenseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}

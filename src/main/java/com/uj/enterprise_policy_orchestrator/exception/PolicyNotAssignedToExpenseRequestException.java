package com.uj.enterprise_policy_orchestrator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
    value = HttpStatus.BAD_REQUEST,
    reason = "Selected policy is not assigned to the expense request")
public class PolicyNotAssignedToExpenseRequestException extends RuntimeException {

  public PolicyNotAssignedToExpenseRequestException(Long requestId, Long policyId) {
    super("Policy %d is not assigned to expense request %d".formatted(policyId, requestId));
  }
}

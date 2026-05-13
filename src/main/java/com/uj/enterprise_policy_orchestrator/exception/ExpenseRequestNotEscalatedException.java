package com.uj.enterprise_policy_orchestrator.exception;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Expense request is not escalated")
public class ExpenseRequestNotEscalatedException extends RuntimeException {

  public ExpenseRequestNotEscalatedException(Long requestId, ExpenseRequestStatus currentStatus) {
    super(
        "Expense request %d must be ESCALATED. Current status: %s"
            .formatted(requestId, currentStatus));
  }
}

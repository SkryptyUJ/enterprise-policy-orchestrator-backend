package com.uj.enterprise_policy_orchestrator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Decline, no matching policies")
public class NoApplicablePoliciesException extends RuntimeException {

  public NoApplicablePoliciesException() {
    super("Decline, no matching policies");
  }
}

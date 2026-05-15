package com.uj.enterprise_policy_orchestrator.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Manager role is required")
public class ManagerRoleRequiredException extends RuntimeException {

  public ManagerRoleRequiredException() {
    super("Manager role is required");
  }
}

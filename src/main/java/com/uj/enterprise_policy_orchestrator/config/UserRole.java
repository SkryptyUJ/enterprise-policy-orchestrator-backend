package com.uj.enterprise_policy_orchestrator.config;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
  ADMIN("admin", 4),
  EMPLOYEE("employee", 1),
  MANAGER("manager", 2),
  COMPLIANCE_OFFICER("compliance_officer", 3);

  private final String authorityName;
  private final Integer policyRoleId;

  UserRole(String authorityName, Integer policyRoleId) {
    this.authorityName = authorityName;
    this.policyRoleId = policyRoleId;
  }

  public String authorityName() {
    return authorityName;
  }

  public Integer policyRoleId() {
    return policyRoleId;
  }

  public static Optional<UserRole> fromAuthorityName(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    return Arrays.stream(values())
        .filter(role -> role.authorityName.equals(value.trim()))
        .findFirst();
  }
}

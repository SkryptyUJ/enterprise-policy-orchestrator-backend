package com.uj.enterprise_policy_orchestrator.dto;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseRequestDetailsDto(
    Long id,
    String userId,
    BigDecimal amount,
    String category,
    String description,
    LocalDateTime expenseDate,
    LocalDateTime submittedAt,
    ExpenseRequestStatus status,
    Long resolutionPolicyId,
    List<ExpenseRequestPolicyOptionDto> conflictingPolicies) {}

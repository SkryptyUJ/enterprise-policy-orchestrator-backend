package com.uj.enterprise_policy_orchestrator.expense_request.dto;

import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.policy.dto.PolicyDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseRequestDto(
    Long id,
    String userId,
    BigDecimal amount,
    String category,
    String description,
    LocalDateTime expenseDate,
    LocalDateTime submittedAt,
    ExpenseRequestStatus status,
    PolicyDto appliedPolicy,
    List<String> conflictingPolicyNames,
    String decisionRationale,
    String decidedBy,
    LocalDateTime decidedAt) {}

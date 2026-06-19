package com.uj.enterprise_policy_orchestrator.expense_request.dto;

import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseRequestDto(
    Long id,
    String userId,
    BigDecimal amount,
    Integer categoryId,
    String categoryLabel,
    String description,
    LocalDateTime expenseDate,
    LocalDateTime submittedAt,
    ExpenseRequestStatus status,
    List<String> applicablePolicies,
    String decisionRationale,
    String decidedBy,
    LocalDateTime decidedAt) {}

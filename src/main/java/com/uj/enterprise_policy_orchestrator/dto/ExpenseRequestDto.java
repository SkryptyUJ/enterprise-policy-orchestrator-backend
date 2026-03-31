package com.uj.enterprise_policy_orchestrator.dto;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseRequestDto(
    Long id,
    Long userId,
    BigDecimal amount,
    String category,
    String description,
    LocalDate expenseDate,
    LocalDateTime submittedAt,
    ExpenseRequestStatus status) {}

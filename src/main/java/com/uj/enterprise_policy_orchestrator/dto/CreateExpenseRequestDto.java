package com.uj.enterprise_policy_orchestrator.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateExpenseRequestDto(
    BigDecimal amount, String category, String description, LocalDateTime expenseDate) {}

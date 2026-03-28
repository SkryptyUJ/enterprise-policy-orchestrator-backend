package com.uj.enterprise_policy_orchestrator.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequestDto(
    BigDecimal amount, String category, String description, LocalDate expenseDate) {}

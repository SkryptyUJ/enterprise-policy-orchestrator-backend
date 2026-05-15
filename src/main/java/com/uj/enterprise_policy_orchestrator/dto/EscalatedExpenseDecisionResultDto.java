package com.uj.enterprise_policy_orchestrator.dto;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;

public record EscalatedExpenseDecisionResultDto(
    Long requestId, ExpenseRequestStatus status, Long selectedPolicyId, String selectedPolicyRef) {}

package com.uj.enterprise_policy_orchestrator.dto;

import com.uj.enterprise_policy_orchestrator.domain.enums.ManagerDecision;

public record EscalatedExpenseDecisionDto(Long policyId, ManagerDecision decision) {}

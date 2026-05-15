package com.uj.enterprise_policy_orchestrator.dto;

public record ExpenseRequestPolicyOptionDto(
    Long id, String policyId, String name, String description) {}

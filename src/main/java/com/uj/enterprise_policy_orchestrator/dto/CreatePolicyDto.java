package com.uj.enterprise_policy_orchestrator.dto;

import java.time.LocalDate;

public record CreatePolicyDto(
    String policyId,
    Integer categoryId,
    String name,
    String description,
    LocalDate startsAt,
    LocalDate expiresAt,
    Integer minPrice,
    Integer maxPrice,
    String category,
    Integer authorizedRole) {}

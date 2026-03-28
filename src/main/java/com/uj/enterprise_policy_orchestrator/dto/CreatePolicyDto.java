package com.uj.enterprise_policy_orchestrator.dto;

import java.time.LocalDateTime;

public record CreatePolicyDto(
    Long policyId,
    Integer categoryId,
    String name,
    String description,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    Integer minPrice,
    Integer maxPrice,
    Integer category,
    Integer authorizedRole) {}

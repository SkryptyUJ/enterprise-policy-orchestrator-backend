package com.uj.enterprise_policy_orchestrator.dto;

import java.time.LocalDateTime;

public record PolicyDto(
    Long id,
    Long policyId,
    Long authorUserId,
    Integer categoryId,
    String name,
    String description,
    Integer version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    Integer minPrice,
    Integer maxPrice,
    Integer category,
    Integer authorizedRole,
    Boolean isValid) {}

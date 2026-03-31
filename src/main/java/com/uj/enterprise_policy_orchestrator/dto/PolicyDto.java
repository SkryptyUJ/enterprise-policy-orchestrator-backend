package com.uj.enterprise_policy_orchestrator.dto;

import java.time.LocalDate;
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
    LocalDate startsAt,
    LocalDate expiresAt,
    Integer minPrice,
    Integer maxPrice,
    String category,
    Integer authorizedRole,
    Boolean isValid) {}

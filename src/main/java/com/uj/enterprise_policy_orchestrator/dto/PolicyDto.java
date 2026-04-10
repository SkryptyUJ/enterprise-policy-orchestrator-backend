package com.uj.enterprise_policy_orchestrator.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record PolicyDto(
    Long id,
    String policyId,
    String authorUserId,
    Integer categoryId,
    String name,
    String description,
    Integer version,
    LocalTime createdAt,
    LocalTime updatedAt,
    LocalDate startsAt,
    LocalDate expiresAt,
    Integer minPrice,
    Integer maxPrice,
    String category,
    Integer authorizedRole,
    Boolean isValid) {}

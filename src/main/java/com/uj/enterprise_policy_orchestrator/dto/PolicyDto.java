package com.uj.enterprise_policy_orchestrator.dto;

import java.math.BigInteger;
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
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    BigInteger minPrice,
    BigInteger maxPrice,
    Integer category,
    Integer authorizedRole) {}

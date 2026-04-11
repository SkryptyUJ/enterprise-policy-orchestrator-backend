package com.uj.enterprise_policy_orchestrator.dto;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;

public record CreatePolicyDto(
    Optional<String> policyId,
    Integer categoryId,
    String name,
    String description,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    BigInteger minPrice,
    BigInteger maxPrice,
    Integer category,
    Integer authorizedRole) {}

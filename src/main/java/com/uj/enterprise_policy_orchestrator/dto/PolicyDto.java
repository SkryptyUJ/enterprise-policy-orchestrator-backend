package com.uj.enterprise_policy_orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PolicyDto(
    Long id,
    String policyId,
    String authorUserId,
    Integer categoryId,
    String name,
    String description,
    Integer version,
    LocalDateTime updatedAt,
    LocalDateTime createdAt,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String category,
        Integer authorizedRole) {

    @JsonProperty("active")
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !startsAt.isAfter(now) && (expiresAt == null || expiresAt.isAfter(now));
    }
}

package com.uj.enterprise_policy_orchestrator.dto;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import java.time.LocalDateTime;

public record ExpenseRequestHistoryDto(
    Long id,
    Long requestId,
    String userId,
    ExpenseRequestStatus previousStatus,
    ExpenseRequestStatus newStatus,
    LocalDateTime changedAt,
    String changeReason) {}

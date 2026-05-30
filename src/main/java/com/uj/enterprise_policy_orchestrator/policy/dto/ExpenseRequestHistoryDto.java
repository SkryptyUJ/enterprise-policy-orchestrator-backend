package com.uj.enterprise_policy_orchestrator.policy.dto;

import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import java.time.LocalDateTime;

public record ExpenseRequestHistoryDto(
    Long id,
    Long requestId,
    String userId,
    ExpenseRequestStatus previousStatus,
    ExpenseRequestStatus newStatus,
    LocalDateTime changedAt,
    String changeReason) {}

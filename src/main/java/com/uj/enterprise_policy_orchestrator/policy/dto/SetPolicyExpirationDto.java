package com.uj.enterprise_policy_orchestrator.policy.dto;

import java.time.LocalDateTime;

public record SetPolicyExpirationDto(LocalDateTime expiresAt) {}

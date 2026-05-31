package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uj.enterprise_policy_orchestrator.policy.Policy;
import com.uj.enterprise_policy_orchestrator.policy.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.policy.repository.PolicyRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Timezone Integration Tests")
class TimezoneIntegrationTest {

  @Autowired private PolicyRepository policyRepository;

  private static final String EXPECTED_TIMEZONE = "Europe/Warsaw";

  @BeforeEach
  void setUp() {
    policyRepository.deleteAll();
  }

  @Test
  @DisplayName("should verify application is configured with Europe/Warsaw timezone")
  void shouldVerifyTimezoneConfiguration() {
    String currentTimezone = TimeZone.getDefault().getID();
    assertThat(currentTimezone).isEqualTo(EXPECTED_TIMEZONE);
  }

  @Test
  @DisplayName(
      "policy should be active when current time is between startsAt and expiresAt in Poland timezone")
  void shouldMarkPolicyAsActiveBetweenStartAndExpire() {
    LocalDateTime startsAt = LocalDateTime.of(2026, 5, 31, 16, 26, 0);
    LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 1, 16, 26, 0);
    LocalDateTime createdAt = LocalDateTime.of(2026, 5, 31, 14, 27, 7);

    Policy policy =
        Policy.builder()
            .policyId("policy-tz-test-1")
            .authorUserId("user-tz-test")
            .categoryId(1)
            .name("Timezone Test Policy")
            .description("Test policy for timezone handling")
            .version(1)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .startsAt(startsAt)
            .expiresAt(expiresAt)
            .minPrice(new BigDecimal("1.00"))
            .maxPrice(new BigDecimal("1000.00"))
            .category("1")
            .authorizedRole(2)
            .build();

    policyRepository.save(policy);

    // when: Convert to DTO (which computes active flag)
    Policy retrieved = policyRepository.findById(policy.getId()).orElseThrow();
    PolicyDto dto =
        new PolicyDto(
            retrieved.getId(),
            retrieved.getPolicyId(),
            retrieved.getAuthorUserId(),
            retrieved.getCategoryId(),
            retrieved.getName(),
            retrieved.getDescription(),
            retrieved.getVersion(),
            retrieved.getUpdatedAt(),
            retrieved.getCreatedAt(),
            retrieved.getStartsAt(),
            retrieved.getExpiresAt(),
            retrieved.getMinPrice(),
            retrieved.getMaxPrice(),
            retrieved.getCategory(),
            retrieved.getAuthorizedRole());

    // then: Policy should be active
    assertThat(dto.isActive())
        .as(
            "Policy starting at 16:26 should be active at 16:50 Poland time. "
                + "startsAt=%s, expiresAt=%s, now=%s",
            dto.startsAt(), dto.expiresAt(), LocalDateTime.now())
        .isTrue();
  }

  @Test
  @DisplayName("policy should be inactive if it has not started yet")
  void shouldMarkPolicyAsInactiveBeforeStart() {
    // given: Create a policy that starts in the future
    LocalDateTime startsAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
    LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 2, 0, 0, 0);
    LocalDateTime createdAt = LocalDateTime.of(2026, 5, 31, 14, 27, 7);

    Policy policy =
        Policy.builder()
            .policyId("policy-future-test")
            .authorUserId("user-tz-test")
            .categoryId(1)
            .name("Future Timezone Test Policy")
            .description("Test policy for future timezone handling")
            .version(1)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .startsAt(startsAt)
            .expiresAt(expiresAt)
            .minPrice(new BigDecimal("1.00"))
            .maxPrice(new BigDecimal("1000.00"))
            .category("1")
            .authorizedRole(2)
            .build();

    policyRepository.save(policy);

    // when: Convert to DTO
    Policy retrieved = policyRepository.findById(policy.getId()).orElseThrow();
    PolicyDto dto =
        new PolicyDto(
            retrieved.getId(),
            retrieved.getPolicyId(),
            retrieved.getAuthorUserId(),
            retrieved.getCategoryId(),
            retrieved.getName(),
            retrieved.getDescription(),
            retrieved.getVersion(),
            retrieved.getUpdatedAt(),
            retrieved.getCreatedAt(),
            retrieved.getStartsAt(),
            retrieved.getExpiresAt(),
            retrieved.getMinPrice(),
            retrieved.getMaxPrice(),
            retrieved.getCategory(),
            retrieved.getAuthorizedRole());

    // then: Policy should be inactive
    assertThat(dto.isActive()).as("Policy starting tomorrow should be inactive today").isFalse();
  }

  @Test
  @DisplayName("policy matching should work correctly with timezone-aware dates")
  void shouldMatchPoliciesByDateWithTimezoneAwareness() {
    // given: Create a policy that covers today
    LocalDateTime startsAt = LocalDateTime.of(2026, 5, 31, 0, 0, 0);
    LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 1, 23, 59, 59);

    Policy policy =
        Policy.builder()
            .policyId("policy-match-test")
            .authorUserId("user-tz-test")
            .categoryId(1)
            .name("Matching Test Policy")
            .description("Test policy for date matching")
            .version(1)
            .createdAt(LocalDateTime.of(2026, 5, 31, 14, 0, 0))
            .updatedAt(LocalDateTime.of(2026, 5, 31, 14, 0, 0))
            .startsAt(startsAt)
            .expiresAt(expiresAt)
            .minPrice(new BigDecimal("100.00"))
            .maxPrice(new BigDecimal("5000.00"))
            .category("1")
            .authorizedRole(2)
            .build();

    policyRepository.save(policy);

    // when: Search for policies that match today's date
    LocalDateTime queryDate =
        LocalDateTime.of(2026, 5, 31, 16, 50, 0); // Current time approximation
    java.util.List<Policy> found =
        policyRepository.findByCategoryAndDateAndAmount("1", queryDate, new BigDecimal("200.00"));

    // then: Policy should be found as applicable
    assertThat(found).isNotEmpty().contains(policy);
  }
}

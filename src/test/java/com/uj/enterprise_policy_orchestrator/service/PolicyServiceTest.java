package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService")
class PolicyServiceTest {

  @Mock private PolicyRepository policyRepository;
  @InjectMocks private PolicyService policyService;

  @Nested
  @DisplayName("Scenario 1: User creates a valid policy")
  class CreatePolicy {

    @Test
    @DisplayName("should create a policy with given data and automatic timestamps")
    void shouldCreatePolicyWithTimestamps() {
      String userId = "1";

      LocalDate startsAt = LocalDate.of(2026, 4, 1);
      LocalDate expiresAt = LocalDate.of(2027, 3, 31);

      CreatePolicyDto dto =
          new CreatePolicyDto(
              "100",
              1,
              "Travel Policy",
              "Company travel expense policy",
              startsAt,
              expiresAt,
              100,
              5000,
              "Travel",
              2);

      when(policyRepository.findByPolicyId("100")).thenReturn(List.of());
      when(policyRepository.save(any(Policy.class)))
          .thenAnswer(
              invocation -> {
                Policy policy = invocation.getArgument(0);
                policy.setId(1L);
                return policy;
              });

      PolicyDto result = policyService.createPolicy(userId, dto);

      assertThat(result.id()).isEqualTo(1L);
      assertThat(result.authorUserId()).isEqualTo(userId);
      assertThat(result.policyId()).isEqualTo("100");
      assertThat(result.name()).isEqualTo("Travel Policy");
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("should persist the policy in the database")
    void shouldPersistPolicyInDatabase() {
      String userId = "2";
      LocalDate startsAt = LocalDate.of(2026, 5, 1);

      CreatePolicyDto dto =
          new CreatePolicyDto(
              "200",
              2,
              "Hardware Policy",
              "Computer equipment policy",
              startsAt,
              null,
              500,
              10000,
              "Hardware",
              3);

      when(policyRepository.findByPolicyId("200")).thenReturn(List.of());
      when(policyRepository.save(any(Policy.class)))
          .thenAnswer(
              invocation -> {
                Policy policy = invocation.getArgument(0);
                policy.setId(2L);
                return policy;
              });

      policyService.createPolicy(userId, dto);

      ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
      verify(policyRepository, times(1)).save(captor.capture());

      Policy saved = captor.getValue();
      assertThat(saved.getAuthorUserId()).isEqualTo(userId);
      assertThat(saved.getPolicyId()).isEqualTo("200");
      assertThat(saved.getName()).isEqualTo("Hardware Policy");
      assertThat(saved.getCategoryId()).isEqualTo(2);
      assertThat(saved.getCategory()).isEqualTo("Hardware");
      assertThat(saved.getIsValid()).isTrue();
    }
  }

  @Nested
  @DisplayName("Scenario 2: User retrieves an existing policy")
  class GetPolicy {

    @Test
    @DisplayName("should retrieve policy by id")
    void shouldRetrievePolicyById() {
      Long policyId = 1L;
      LocalDate nowDate = LocalDate.now();
      LocalTime now = LocalTime.now();
      Policy policy =
          Policy.builder()
              .id(policyId)
              .policyId("100")
              .authorUserId("5")
              .categoryId(1)
              .name("Test Policy")
              .description("Test Description")
              .version(1)
              .createdAt(now)
              .updatedAt(now)
              .startsAt(nowDate.plusDays(1))
              .expiresAt(nowDate.plusYears(1))
              .minPrice(100)
              .maxPrice(5000)
              .category("TestCategory")
              .authorizedRole(2)
              .isValid(true)
              .build();

      when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

      PolicyDto result = policyService.getPolicyById(policyId);

      assertThat(result.id()).isEqualTo(policyId);
      assertThat(result.name()).isEqualTo("Test Policy");
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("should throw exception when policy not found")
    void shouldThrowWhenPolicyNotFound() {
      Long nonExistentPolicyId = 999L;
      when(policyRepository.findById(nonExistentPolicyId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> policyService.getPolicyById(nonExistentPolicyId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("999");
    }
  }

  @Nested
  @DisplayName("Scenario 3: Determine applicable policies")
  class FindApplicablePolicies {

    private final LocalDate expenseDate = LocalDate.of(2026, 4, 1);
    private final BigDecimal amount = BigDecimal.valueOf(750);

    @Test
    @DisplayName("should return only the latest version per policyId")
    void shouldReturnLatestVersionPerPolicyId() {
      Policy olderVersion =
          policy(
              1L,
              "500",
              "Travel",
              expenseDate.minusDays(5),
              expenseDate.plusDays(10),
              1,
              100,
              1500,
              LocalTime.of(10, 0));
      Policy newerVersion =
          policy(
              2L,
              "500",
              "Travel",
              expenseDate.minusDays(5),
              expenseDate.plusDays(10),
              2,
              100,
              1500,
              LocalTime.of(11, 0));
      Policy otherPolicy =
          policy(
              3L,
              "600",
              "Equipment",
              expenseDate.minusDays(2),
              expenseDate.plusDays(30),
              1,
              200,
              5000,
              LocalTime.of(9, 0));

      when(policyRepository.findByCategoryAndDateAndAmount("Travel", expenseDate, amount))
          .thenReturn(List.of(olderVersion, newerVersion, otherPolicy));

      Set<Policy> result = policyService.findApplicablePolicies("Travel", expenseDate, amount);

      assertThat(result).containsExactlyInAnyOrder(newerVersion, otherPolicy);
    }

    @Test
    @DisplayName("should delegate filter parameters to repository")
    void shouldDelegateFiltersToRepository() {
      when(policyRepository.findByCategoryAndDateAndAmount("Hardware", expenseDate, amount))
          .thenReturn(List.of());

      policyService.findApplicablePolicies("Hardware", expenseDate, amount);

      verify(policyRepository).findByCategoryAndDateAndAmount("Hardware", expenseDate, amount);
    }

    @Test
    @DisplayName("should return empty set when no policies match filters")
    void shouldReturnEmptySetWhenNoMatches() {
      when(policyRepository.findByCategoryAndDateAndAmount("Travel", expenseDate, amount))
          .thenReturn(List.of());

      Set<Policy> result = policyService.findApplicablePolicies("Travel", expenseDate, amount);

      assertThat(result).isEmpty();
    }
  }

  private Policy policy(
      Long id,
      String policyId,
      String category,
      LocalDate startsAt,
      LocalDate expiresAt,
      Integer version,
      Integer minPrice,
      Integer maxPrice,
      LocalTime updatedAt) {
    return Policy.builder()
        .id(id)
        .policyId(policyId)
        .authorUserId("1")
        .categoryId(1)
        .name("Policy " + policyId)
        .description("Description for " + policyId)
        .version(version)
        .createdAt(updatedAt.minusHours(1))
        .updatedAt(updatedAt)
        .startsAt(startsAt)
        .expiresAt(expiresAt)
        .minPrice(minPrice)
        .maxPrice(maxPrice)
        .category(category)
        .authorizedRole(1)
        .isValid(true)
        .build();
  }
}

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
      // @TODO: Restore when user check is re-enabled in PolicyService.createPolicy()
      // User author = User.builder().id(userId).username("admin.user").build();

      LocalDateTime startsAt = LocalDateTime.of(2026, 4, 1, 0, 0, 0);
      LocalDateTime expiresAt = LocalDateTime.of(2027, 3, 31, 23, 59, 59);

      CreatePolicyDto dto =
          new CreatePolicyDto(
              Optional.of("100"),
              1,
              "Travel Policy",
              "Company travel expense policy",
              startsAt,
              expiresAt,
              new java.math.BigInteger("100"),
              new java.math.BigInteger("5000"),
              1,
              2);

      // @TODO: Restore when user check is re-enabled in PolicyService.createPolicy()
      // when(userRepository.findById(userId)).thenReturn(Optional.of(author));
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
    }

    @Test
    @DisplayName("should persist the policy in the database")
    void shouldPersistPolicyInDatabase() {
      String userId = "2";
      // @TODO: Restore when user check is re-enabled in PolicyService.createPolicy()
      // User author = User.builder().id(userId).username("policy.creator").build();
      LocalDateTime startsAt = LocalDateTime.of(2026, 5, 1, 0, 0, 0);

      CreatePolicyDto dto =
          new CreatePolicyDto(
              Optional.of("200"),
              2,
              "Hardware Policy",
              "Computer equipment policy",
              startsAt,
              null,
              new java.math.BigInteger("500"),
              new java.math.BigInteger("10000"),
              2,
              3);

      // @TODO: Restore when user check is re-enabled in PolicyService.createPolicy()
      // when(userRepository.findById(userId)).thenReturn(Optional.of(author));
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
      assertThat(saved.getCategory()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("Scenario 2: User retrieves an existing policy")
  class GetPolicy {

    @Test
    @DisplayName("should retrieve policy by policyId")
    void shouldRetrievePolicyByPolicyId() {
      String policyId = "POL-100";
      LocalDateTime now = LocalDateTime.now();
      Policy policy =
          Policy.builder()
              .id(1L)
              .policyId(policyId)
              .authorUserId("5")
              .categoryId(1)
              .name("Test Policy")
              .description("Test Description")
              .version(1)
              .createdAt(now)
              .startsAt(now.plusDays(1))
              .expiresAt(now.plusYears(1))
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("5000"))
              .category(1)
              .authorizedRole(2)
              .build();

      when(policyRepository.findFirstByPolicyIdOrderByVersionDesc(policyId))
          .thenReturn(Optional.of(policy));

      PolicyDto result = policyService.getPolicyByPolicyId(policyId);

      assertThat(result.policyId()).isEqualTo(policyId);
      assertThat(result.name()).isEqualTo("Test Policy");
    }

    @Test
    @DisplayName("should throw exception when policy not found")
    void shouldThrowWhenPolicyNotFound() {
      String missingPolicyId = "MISSING";
      when(policyRepository.findFirstByPolicyIdOrderByVersionDesc(missingPolicyId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> policyService.getPolicyByPolicyId(missingPolicyId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("MISSING");
    }
  }

  @Nested
  @DisplayName("Scenario 3: User retrieves applicable policies for expense")
  class FindApplicablePolicies {

    @Test
    @DisplayName("should find policies matching category, date, and amount range")
    void shouldFindPoliciesMatchingCategoryDateAndAmount() {
      // given
      String category = "Travel";
      java.time.LocalDate expenseDate = java.time.LocalDate.of(2026, 3, 15);
      java.math.BigDecimal amount = new java.math.BigDecimal("2500.00");
      LocalDateTime now = LocalDateTime.now();

      Policy policy1 =
          Policy.builder()
              .id(1L)
              .policyId("TRAVEL-001")
              .authorUserId("1")
              .categoryId(1)
              .name("Travel Policy 1")
              .version(1)
              .createdAt(now)
              .startsAt(now.minusDays(30))
              .expiresAt(now.plusDays(365))
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("5000"))
              .category(1)
              .authorizedRole(1)
              .updatedAt(now)
              .build();

      Policy policy2 =
          Policy.builder()
              .id(2L)
              .policyId("TRAVEL-002")
              .authorUserId("2")
              .categoryId(1)
              .name("Travel Policy 2")
              .version(1)
              .createdAt(now)
              .startsAt(now.minusDays(60))
              .expiresAt(null)
              .minPrice(new java.math.BigInteger("500"))
              .maxPrice(new java.math.BigInteger("10000"))
              .category(1)
              .authorizedRole(2)
              .updatedAt(now.minusDays(10))
              .build();

      when(policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount))
          .thenReturn(List.of(policy1, policy2));

      // when
      java.util.Set<Policy> result =
          policyService.findApplicablePolicies(category, expenseDate, amount);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).contains(policy1, policy2);
    }

    @Test
    @DisplayName("should return empty set when no policies match")
    void shouldReturnEmptySetWhenNoPoliciesMatch() {
      // given
      String category = "NonExistent";
      java.time.LocalDate expenseDate = java.time.LocalDate.of(2026, 3, 15);
      java.math.BigDecimal amount = new java.math.BigDecimal("1000.00");

      when(policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount))
          .thenReturn(List.of());

      // when
      java.util.Set<Policy> result =
          policyService.findApplicablePolicies(category, expenseDate, amount);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should handle single applicable policy")
    void shouldHandleSingleApplicablePolicy() {
      // given
      String category = "Office";
      java.time.LocalDate expenseDate = java.time.LocalDate.of(2026, 4, 1);
      java.math.BigDecimal amount = new java.math.BigDecimal("150.00");
      LocalDateTime now = LocalDateTime.now();

      Policy policy =
          Policy.builder()
              .id(1L)
              .policyId("OFFICE-001")
              .authorUserId("1")
              .categoryId(2)
              .name("Office Policy")
              .version(1)
              .createdAt(now)
              .startsAt(now.minusDays(1))
              .expiresAt(null)
              .minPrice(java.math.BigInteger.ZERO)
              .maxPrice(new java.math.BigInteger("500"))
              .category(2)
              .authorizedRole(1)
              .updatedAt(now)
              .build();

      when(policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount))
          .thenReturn(List.of(policy));

      // when
      java.util.Set<Policy> result =
          policyService.findApplicablePolicies(category, expenseDate, amount);

      // then
      assertThat(result).hasSize(1);
      assertThat(result).contains(policy);
    }

    @Test
    @DisplayName("should select latest version when multiple versions exist for same policyId")
    void shouldSelectLatestVersionWhenMultipleVersionsExist() {
      // given
      String category = "Training";
      java.time.LocalDate expenseDate = java.time.LocalDate.of(2026, 5, 1);
      java.math.BigDecimal amount = new java.math.BigDecimal("500.00");
      LocalDateTime now = LocalDateTime.now();

      // Old version of TRAINING-001
      Policy trainingV1 =
          Policy.builder()
              .id(1L)
              .policyId("TRAINING-001")
              .authorUserId("1")
              .categoryId(3)
              .name("Training Policy v1")
              .version(1)
              .createdAt(now.minusDays(10))
              .startsAt(now.minusDays(10))
              .expiresAt(now.plusDays(355))
              .minPrice(new java.math.BigInteger("50"))
              .maxPrice(new java.math.BigInteger("1000"))
              .category(3)
              .authorizedRole(1)
              .updatedAt(now.minusDays(10))
              .build();

      // New version of TRAINING-001
      Policy trainingV2 =
          Policy.builder()
              .id(2L)
              .policyId("TRAINING-001")
              .authorUserId("1")
              .categoryId(3)
              .name("Training Policy v2")
              .version(2)
              .createdAt(now.minusDays(5))
              .startsAt(now.minusDays(5))
              .expiresAt(null)
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("1500"))
              .category(3)
              .authorizedRole(1)
              .updatedAt(now) // More recent
              .build();

      when(policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount))
          .thenReturn(List.of(trainingV1, trainingV2));

      // when
      java.util.Set<Policy> result =
          policyService.findApplicablePolicies(category, expenseDate, amount);

      // then — only the latest version should be included
      assertThat(result).hasSize(1);
      assertThat(result).contains(trainingV2);
      assertThat(result).doesNotContain(trainingV1);
    }

    @Test
    @DisplayName("should handle mixed versions from different policies")
    void shouldHandleMixedVersionsFromDifferentPolicies() {
      // given
      String category = "Meals";
      java.time.LocalDate expenseDate = java.time.LocalDate.of(2026, 6, 1);
      java.math.BigDecimal amount = new java.math.BigDecimal("100.00");
      LocalDateTime now = LocalDateTime.now();

      // Latest version of MEALS-001
      Policy mealsPolicy1 =
          Policy.builder()
              .id(1L)
              .policyId("MEALS-001")
              .authorUserId("1")
              .categoryId(4)
              .name("Meals Policy v2")
              .version(2)
              .createdAt(now.minusDays(10))
              .startsAt(now.minusDays(10))
              .expiresAt(null)
              .minPrice(java.math.BigInteger.ZERO)
              .maxPrice(new java.math.BigInteger("200"))
              .category(4)
              .authorizedRole(1)
              .updatedAt(now)
              .build();

      // Latest version of MEALS-002
      Policy mealsPolicy2 =
          Policy.builder()
              .id(2L)
              .policyId("MEALS-002")
              .authorUserId("2")
              .categoryId(4)
              .name("Meals Policy 2")
              .version(1)
              .createdAt(now.minusDays(5))
              .startsAt(now.minusDays(5))
              .expiresAt(null)
              .minPrice(java.math.BigInteger.ZERO)
              .maxPrice(new java.math.BigInteger("150"))
              .category(4)
              .authorizedRole(1)
              .updatedAt(now.minusDays(1))
              .build();

      when(policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount))
          .thenReturn(List.of(mealsPolicy1, mealsPolicy2));

      // when
      java.util.Set<Policy> result =
          policyService.findApplicablePolicies(category, expenseDate, amount);

      // then
      assertThat(result).hasSize(2);
      assertThat(result).contains(mealsPolicy1, mealsPolicy2);
    }
  }

  @Nested
  @DisplayName("Scenario 4: User retrieves policy history")
  class GetPolicyHistory {

    @Test
    @DisplayName("should return policy history by policyId")
    void shouldReturnPolicyHistory() {
      String policyId = "ABC-123";
      LocalDateTime now = LocalDateTime.now();

      Policy v2 =
          Policy.builder()
              .id(2L)
              .policyId(policyId)
              .authorUserId("5")
              .categoryId(1)
              .name("Policy v2")
              .description("Updated")
              .version(2)
              .createdAt(now)
              .startsAt(now.plusDays(1))
              .expiresAt(null)
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("5000"))
              .category(1)
              .authorizedRole(2)
              .build();

      Policy v1 =
          Policy.builder()
              .id(1L)
              .policyId(policyId)
              .authorUserId("5")
              .categoryId(1)
              .name("Policy v1")
              .description("Initial")
              .version(1)
              .createdAt(now.minusDays(1))
              .startsAt(now.minusDays(1))
              .expiresAt(now.plusDays(1))
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("5000"))
              .category(1)
              .authorizedRole(2)
              .build();

      when(policyRepository.findByPolicyIdOrderByVersionDesc(policyId)).thenReturn(List.of(v2, v1));

      List<PolicyDto> result = policyService.getPolicyHistory(policyId);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).version()).isEqualTo(2);
      assertThat(result.get(1).version()).isEqualTo(1);
    }

    @Test
    @DisplayName("should throw exception when history is empty")
    void shouldThrowWhenHistoryEmpty() {
      String policyId = "MISSING";
      when(policyRepository.findByPolicyIdOrderByVersionDesc(policyId)).thenReturn(List.of());

      assertThatThrownBy(() -> policyService.getPolicyHistory(policyId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining(policyId);
    }
  }
}

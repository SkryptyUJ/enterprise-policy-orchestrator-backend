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
import com.uj.enterprise_policy_orchestrator.repository.UserRepository;
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
  @Mock private UserRepository userRepository;
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

    // @TODO: Restore when user check is re-enabled in PolicyService.createPolicy()
    // @Test
    // @DisplayName("should throw exception when the user does not exist")
    // void shouldThrowWhenUserNotFound() {
    // Long nonExistentUserId = 999L;
    // LocalDateTime startsAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
    // CreatePolicyDto dto = new CreatePolicyDto(
    // "300",
    // 1,
    // "Test Policy",
    // "Test",
    // startsAt,
    // null,
    // new java.math.BigInteger("0"),
    // new java.math.BigInteger("1000"),
    // 1,
    // 1);
    //
    // when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());
    //
    // assertThatThrownBy(() -> policyService.createPolicy(nonExistentUserId, dto))
    // .isInstanceOf(EntityNotFoundException.class)
    // .hasMessageContaining("999");
    // }
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

      when(policyRepository.findByPolicyIdOrderByVersionDesc(policyId))
          .thenReturn(List.of(v2, v1));

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

  @Nested
  @DisplayName("Scenario 3: Setting end date on an active policy")
  class SetExpiration {

    @Test
    @DisplayName("should update the policy with the given expiration date")
    void shouldUpdatePolicyWithExpirationDate() {
      Long policyId = 1L;
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime expiresAt = now.plusMonths(6);

      Policy policy =
          Policy.builder()
              .id(policyId)
              .policyId("100")
              .authorUserId("1")
              .categoryId(1)
              .name("Active Policy")
              .description("Policy without end date")
              .version(1)
              .createdAt(now.minusDays(30))
              .startsAt(now.minusDays(30))
              .expiresAt(null)
              .minPrice(new java.math.BigInteger("100"))
              .maxPrice(new java.math.BigInteger("5000"))
              .category(1)
              .authorizedRole(2)
              .build();

      when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
      when(policyRepository.save(any(Policy.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      PolicyDto result = policyService.setExpiration(policyId, expiresAt);

      assertThat(result.expiresAt()).isEqualTo(expiresAt);

      ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
      verify(policyRepository).save(captor.capture());
      assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("should throw exception when policy not found")
    void shouldThrowWhenPolicyNotFound() {
      Long nonExistentPolicyId = 999L;
      LocalDateTime expiresAt = LocalDateTime.now().plusMonths(6);

      when(policyRepository.findById(nonExistentPolicyId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> policyService.setExpiration(nonExistentPolicyId, expiresAt))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("999");
    }
  }

  @Nested
  @DisplayName("Scenario 4: Retrieving all policies for history view")
  class GetAllPolicies {

    @Test
    @DisplayName("should return all policies including deactivated ones")
    void shouldReturnAllPoliciesIncludingDeactivated() {
      LocalDateTime now = LocalDateTime.now();
      Policy activePolicy =
          Policy.builder()
              .id(1L)
              .policyId("100")
              .authorUserId("1")
              .categoryId(1)
              .name("Active Policy")
              .version(1)
              .createdAt(now)
              .startsAt(now.minusDays(10))
              .expiresAt(null)
              .category(1)
              .authorizedRole(2)
              .build();
      Policy expiredPolicy =
          Policy.builder()
              .id(2L)
              .policyId("200")
              .authorUserId("1")
              .categoryId(1)
              .name("Expired Policy")
              .version(1)
              .createdAt(now.minusYears(2))
              .startsAt(now.minusYears(2))
              .expiresAt(now.minusDays(1))
              .category(1)
              .authorizedRole(2)
              .build();

      when(policyRepository.findAll()).thenReturn(List.of(activePolicy, expiredPolicy));

      List<PolicyDto> result = policyService.getAllPolicies();

      assertThat(result).hasSize(2);
      assertThat(result).extracting(PolicyDto::name)
          .containsExactly("Active Policy", "Expired Policy");
    }
  }
}

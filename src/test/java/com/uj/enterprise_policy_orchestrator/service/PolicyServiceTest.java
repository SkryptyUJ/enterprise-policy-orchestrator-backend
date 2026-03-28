package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.User;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
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
      Long userId = 1L;
      User author = User.builder().id(userId).username("admin.user").build();

      LocalDateTime startsAt = LocalDateTime.of(2026, 4, 1, 0, 0, 0);
      LocalDateTime expiresAt = LocalDateTime.of(2027, 3, 31, 23, 59, 59);

      CreatePolicyDto dto =
          new CreatePolicyDto(
              100L,
              1,
              "Travel Policy",
              "Company travel expense policy",
              startsAt,
              expiresAt,
              100,
              5000,
              1,
              2);

      when(userRepository.findById(userId)).thenReturn(Optional.of(author));
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
      assertThat(result.policyId()).isEqualTo(100L);
      assertThat(result.name()).isEqualTo("Travel Policy");
    }

    @Test
    @DisplayName("should persist the policy in the database")
    void shouldPersistPolicyInDatabase() {
      Long userId = 2L;
      User author = User.builder().id(userId).username("policy.creator").build();
      LocalDateTime startsAt = LocalDateTime.of(2026, 5, 1, 0, 0, 0);

      CreatePolicyDto dto =
          new CreatePolicyDto(
              200L,
              2,
              "Hardware Policy",
              "Computer equipment policy",
              startsAt,
              null,
              500,
              10000,
              2,
              3);

      when(userRepository.findById(userId)).thenReturn(Optional.of(author));
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
      assertThat(saved.getPolicyId()).isEqualTo(200L);
      assertThat(saved.getName()).isEqualTo("Hardware Policy");
      assertThat(saved.getCategory()).isEqualTo(2);
    }

    @Test
    @DisplayName("should throw exception when the user does not exist")
    void shouldThrowWhenUserNotFound() {
      Long nonExistentUserId = 999L;
      LocalDateTime startsAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      CreatePolicyDto dto =
          new CreatePolicyDto(300L, 1, "Test Policy", "Test", startsAt, null, 0, 1000, 1, 1);

      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> policyService.createPolicy(nonExistentUserId, dto))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("999");
    }
  }

  @Nested
  @DisplayName("Scenario 2: User retrieves an existing policy")
  class GetPolicy {

    @Test
    @DisplayName("should retrieve policy by id")
    void shouldRetrievePolicyById() {
      Long policyId = 1L;
      LocalDateTime now = LocalDateTime.now();
      Policy policy =
          Policy.builder()
              .id(policyId)
              .policyId(100L)
              .authorUserId(5L)
              .categoryId(1)
              .name("Test Policy")
              .description("Test Description")
              .version(1)
              .createdAt(now)
              .startsAt(now.plusDays(1))
              .expiresAt(now.plusYears(1))
              .minPrice(100)
              .maxPrice(5000)
              .category(1)
              .authorizedRole(2)
              .build();

      when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));

      PolicyDto result = policyService.getPolicyById(policyId);

      assertThat(result.id()).isEqualTo(policyId);
      assertThat(result.name()).isEqualTo("Test Policy");
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
}

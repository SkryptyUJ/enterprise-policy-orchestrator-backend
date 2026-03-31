package com.uj.enterprise_policy_orchestrator.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
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
  @InjectMocks private PolicyService policyService;

  @Nested
  @DisplayName("Scenario 1: User creates a valid policy")
  class CreatePolicy {

    @Test
    @DisplayName("should create a policy with given data and automatic timestamps")
    void shouldCreatePolicyWithTimestamps() {
      Long userId = 1L;

      LocalDate startsAt = LocalDate.of(2026, 4, 1);
      LocalDate expiresAt = LocalDate.of(2027, 3, 31);

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
              "Travel",
              2);

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
      assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("should persist the policy in the database")
    void shouldPersistPolicyInDatabase() {
      Long userId = 2L;
      LocalDate startsAt = LocalDate.of(2026, 5, 1);

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
              "Hardware",
              3);

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

    @Nested
    @DisplayName("Scenario 2: User retrieves an existing policy")
    class GetPolicy {

      @Test
      @DisplayName("should retrieve policy by id")
      void shouldRetrievePolicyById() {
        Long policyId = 1L;
        LocalDate nowDate = LocalDate.now();
        LocalDateTime now = java.time.LocalDateTime.now();
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
  }
}

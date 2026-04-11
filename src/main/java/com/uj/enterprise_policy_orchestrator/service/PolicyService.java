package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyService {

  private final PolicyRepository policyRepository;

  @Transactional
  public PolicyDto createPolicy(String authorUserId, CreatePolicyDto dto) {
    String policyId = dto.policyId().orElse(UUID.randomUUID().toString());

    // Find and invalidate the active policy with the same policyId
    int nextVersion = 1;
    var existingPolicies = policyRepository.findByPolicyId(policyId);

    if (!existingPolicies.isEmpty()) {
      LocalDateTime now = LocalDateTime.now();
      var activePolicy =
          existingPolicies.stream().filter(p -> isActiveDuringDate(p, now)).findFirst();

      if (activePolicy.isPresent()) {
        Policy active = activePolicy.get();
        active.setExpiresAt(dto.startsAt());
        policyRepository.save(active);
        nextVersion = active.getVersion() + 1;
      }
    }

    Policy policy =
        Policy.builder()
            .policyId(policyId)
            .authorUserId(authorUserId)
            .categoryId(dto.categoryId())
            .name(dto.name())
            .description(dto.description())
            .updatedAt(LocalDateTime.now())
            .startsAt(dto.startsAt())
            .expiresAt(dto.expiresAt())
            .minPrice(dto.minPrice())
            .maxPrice(dto.maxPrice())
            .category(dto.category())
            .authorizedRole(dto.authorizedRole())
            .version(nextVersion)
            .build();

    Policy saved = policyRepository.save(policy);
    return toDto(saved);
  }

  public PolicyDto getPolicyByPolicyId(String policyId) {
    Policy policy =
        policyRepository
            .findFirstByPolicyIdOrderByVersionDesc(policyId)
            .orElseThrow(
                () -> new EntityNotFoundException("Policy not found with policyId: " + policyId));
    return toDto(policy);
  }

  public List<PolicyDto> getPolicyHistory(String policyId) {
    List<Policy> history = policyRepository.findByPolicyIdOrderByVersionDesc(policyId);

    if (history.isEmpty()) {
      throw new EntityNotFoundException("Policy not found with policyId: " + policyId);
    }

    return history.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Set<Policy> findApplicablePolicies(
      String category, LocalDate expenseDate, BigDecimal amount) {
    List<Policy> applicablePolicies =
        policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount);

    Map<String, Policy> latestByPolicyId =
        applicablePolicies.stream()
            .collect(
                Collectors.toMap(
                    Policy::getPolicyId,
                    Function.identity(),
                    BinaryOperator.maxBy(Comparator.comparing(Policy::getUpdatedAt))));

    return new HashSet<>(latestByPolicyId.values());
  }

  private PolicyDto toDto(Policy entity) {
    return new PolicyDto(
        entity.getId(),
        entity.getPolicyId(),
        entity.getAuthorUserId(),
        entity.getCategoryId(),
        entity.getName(),
        entity.getDescription(),
        entity.getVersion(),
        entity.getUpdatedAt(),
        entity.getCreatedAt(),
        entity.getStartsAt(),
        entity.getExpiresAt(),
        entity.getMinPrice(),
        entity.getMaxPrice(),
        entity.getCategory(),
        entity.getAuthorizedRole());
  }

  private boolean isActiveDuringDate(Policy policy, LocalDateTime date) {
    LocalDateTime expiresAt = policy.getExpiresAt();
    return date.isAfter(policy.getStartsAt()) && (expiresAt == null || date.isBefore(expiresAt));
  }
}

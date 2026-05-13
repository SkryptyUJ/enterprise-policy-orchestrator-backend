package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseCategory;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    String normalizedCategory = ExpenseCategory.normalize(dto.category());

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
            .category(normalizedCategory)
            .authorizedRole(dto.authorizedRole())
            .version(nextVersion)
            .build();

    Policy saved = policyRepository.save(policy);
    return toDto(saved);
  }

  public PolicyDto getPolicyByPolicyId(String policyId) {
    Policy policy =
        resolveLatestPolicy(policyId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Policy not found with identifier: " + policyId));
    return toDto(policy);
  }

  public List<PolicyDto> getPolicyHistory(String policyId) {
    String resolvedPolicyId = resolvePolicyIdForHistory(policyId);
    List<Policy> history = policyRepository.findByPolicyIdOrderByVersionDesc(resolvedPolicyId);

    if (history.isEmpty()) {
      throw new EntityNotFoundException("Policy not found with identifier: " + policyId);
    }

    return history.stream().map(this::toDto).collect(Collectors.toList());
  }

  @Transactional
  public PolicyDto setExpiration(Long policyId, LocalDateTime expiresAt) {
    Policy policy =
        policyRepository
            .findById(policyId)
            .orElseThrow(
                () -> new EntityNotFoundException("Policy not found with id: " + policyId));
    policy.setExpiresAt(expiresAt);
    Policy saved = policyRepository.save(policy);
    return toDto(saved);
  }

  public List<PolicyDto> getAllPolicies() {
    Comparator<Policy> latestVersionComparator =
        Comparator.comparing(Policy::getVersion).thenComparing(Policy::getUpdatedAt);

    Map<String, Policy> latestByPolicyId =
      policyRepository.findAll().stream()
        .collect(
          Collectors.toMap(
            Policy::getPolicyId,
            Function.identity(),
            BinaryOperator.maxBy(latestVersionComparator)));

    return latestByPolicyId.values().stream()
      .sorted(Comparator.comparing(Policy::getUpdatedAt).reversed())
      .map(this::toDto)
      .toList();
  }

  public List<PolicyDto> getActivePolicies() {
    return policyRepository.findActivePolicies(LocalDateTime.now()).stream()
        .map(this::toDto)
        .toList();
  }

  public Set<Policy> findApplicablePolicies(
      String category, LocalDateTime expenseDate, BigDecimal amount) {
    String normalizedCategory = ExpenseCategory.normalize(category);
    List<Policy> applicablePolicies =
      policyRepository.findByCategoryAndDateAndAmount(normalizedCategory, expenseDate, amount);

    Comparator<Policy> latestVersionComparator =
      Comparator.comparing(Policy::getVersion).thenComparing(Policy::getUpdatedAt);

    Map<String, Policy> latestByPolicyId =
        applicablePolicies.stream()
            .collect(
                Collectors.toMap(
                    Policy::getPolicyId,
                    Function.identity(),
            BinaryOperator.maxBy(latestVersionComparator)));

    return new HashSet<>(latestByPolicyId.values());
  }

  PolicyDto toDto(Policy entity) {
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

  private Optional<Policy> resolveLatestPolicy(String identifier) {
    Optional<Policy> byEntityId =
        tryParseLong(identifier)
            .flatMap(policyRepository::findById)
            .flatMap(
                policy ->
                    policyRepository
                        .findFirstByPolicyIdOrderByVersionDesc(policy.getPolicyId())
                        .or(() -> Optional.of(policy)));
    if (byEntityId.isPresent()) {
      return byEntityId;
    }

    return policyRepository.findFirstByPolicyIdOrderByVersionDesc(identifier);
  }

  private String resolvePolicyIdForHistory(String identifier) {
    return tryParseLong(identifier)
        .flatMap(policyRepository::findById)
        .map(Policy::getPolicyId)
        .orElse(identifier);
  }

  private Optional<Long> tryParseLong(String value) {
    try {
      return Optional.of(Long.parseLong(value));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }
}

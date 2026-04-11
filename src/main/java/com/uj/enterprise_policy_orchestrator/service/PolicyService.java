package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyService {

  private final PolicyRepository policyRepository;
  private final UserRepository userRepository;

  @Transactional
  public PolicyDto createPolicy(String authorUserId, CreatePolicyDto dto) {
    // @TODO
    // User author =
    // userRepository
    // .findById(authorUserId)
    // .orElseThrow(
    // () -> new EntityNotFoundException("User not found with id: " +
    // authorUserId));

    String policyId = dto.policyId().orElse(UUID.randomUUID().toString());

    // Find and invalidate the active policy with the same policyId
    Integer nextVersion = 1;
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

  public PolicyDto getPolicyById(Long id) {
    Policy policy =
        policyRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Policy not found with id: " + id));
    return toDto(policy);
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
    return policyRepository.findAll().stream().map(this::toDto).toList();
  }

  public List<PolicyDto> getActivePolicies() {
    return policyRepository.findActivePolicies(LocalDateTime.now()).stream()
        .map(this::toDto)
        .toList();
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

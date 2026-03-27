package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.User;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyService {

  private final PolicyRepository policyRepository;
  private final UserRepository userRepository;

  @Transactional
  public PolicyDto createPolicy(Long authorUserId, CreatePolicyDto dto) {
    User author =
        userRepository
            .findById(authorUserId)
            .orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + authorUserId));

    Policy policy =
        Policy.builder()
            .policyId(dto.policyId())
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
            .isValid(true)
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
        entity.getUpdatedAt(),
        entity.getStartsAt(),
        entity.getExpiresAt(),
        entity.getMinPrice(),
        entity.getMaxPrice(),
        entity.getCategory(),
        entity.getAuthorizedRole(),
        entity.getIsValid());
  }
}

package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  public PolicyDto createPolicy(Long authorUserId, CreatePolicyDto dto) {
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

  public Set<Policy> findApplicablePolicies(
      String category, LocalDate expenseDate, BigDecimal amount) {
    List<Policy> applicablePolicies =
        policyRepository.findByCategoryAndDateAndAmount(category, expenseDate, amount);

    Map<Long, Policy> latestByPolicyId =
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

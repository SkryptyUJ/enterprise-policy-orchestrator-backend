package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseCategory;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseRequestService {

  private final ExpenseRequestRepository expenseRequestRepository;
  private final PolicyRepository policyRepository;
  private final PolicyService policyService;

  @Transactional
  public ExpenseRequestDto createExpenseRequest(String userId, CreateExpenseRequestDto dto) {
    String normalizedCategory = ExpenseCategory.normalize(dto.category());

    ExpenseRequest request =
        ExpenseRequest.builder()
            .userId(userId)
            .amount(dto.amount())
            .category(normalizedCategory)
            .description(dto.description())
            .expenseDate(dto.expenseDate())
            .build();

    Set<Policy> applicablePolicies = findApplicablePolicies(request);
    if (applicablePolicies.isEmpty()) {
      request.setStatus(ExpenseRequestStatus.DECLINED);
      throw new NoApplicablePoliciesException(buildNoMatchingPoliciesMessage(request, dto.category()));
    }

    request.getApplicablePolicies().addAll(applicablePolicies);

    ExpenseRequest saved = expenseRequestRepository.save(request);

    return toDto(saved);
  }

  List<Policy> getActivePolicies() {
    return policyRepository.findActivePolicies(LocalDateTime.now());
  }

  private Set<Policy> findApplicablePolicies(ExpenseRequest exp) {
    LocalDateTime expenseDateForMatching = exp.getExpenseDate();
    if (expenseDateForMatching != null && expenseDateForMatching.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      // Date-only input is deserialized to 00:00; match policies against the entire day.
      expenseDateForMatching = expenseDateForMatching.with(LocalTime.MAX);
    }

    return policyService.findApplicablePolicies(
        exp.getCategory(), expenseDateForMatching, exp.getAmount());
  }

  private String buildNoMatchingPoliciesMessage(ExpenseRequest request, String requestedCategoryRaw) {
    LocalDateTime expenseDateForMatching = request.getExpenseDate();
    if (expenseDateForMatching != null && expenseDateForMatching.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      expenseDateForMatching = expenseDateForMatching.with(LocalTime.MAX);
    }

    List<Policy> matchingDateAndAmount =
        policyRepository.findByDateAndAmount(expenseDateForMatching, request.getAmount());

    Set<String> availableCategories = new LinkedHashSet<>();
    matchingDateAndAmount.stream().map(Policy::getCategory).forEach(availableCategories::add);

    if (availableCategories.isEmpty()) {
      return "Decline, no matching policies";
    }

    String categoryForMessage = requestedCategoryRaw == null ? request.getCategory() : requestedCategoryRaw;

    return "Decline, no matching policies for category '"
        + categoryForMessage
        + "'. Available categories for the provided date and amount: "
        + availableCategories;
  }

  @Transactional(readOnly = true)
  public List<ExpenseRequestDto> getExpenseRequestHistory(String userId) {
    // userRepository
    //     .findById(userId)
    //     .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    return expenseRequestRepository
        .findByUserId(userId, Sort.by(Sort.Direction.DESC, "submittedAt"))
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public ExpenseRequestDto getExpenseRequestById(String userId, Long requestId) {
    ExpenseRequest request =
        expenseRequestRepository
            .findById(requestId)
            .orElseThrow(
                () ->
                    new jakarta.persistence.EntityNotFoundException(
                        "Expense request not found with id: " + requestId));

    if (!request.getUserId().equals(userId)) {
      throw new jakarta.persistence.EntityNotFoundException(
          "Expense request not found with id: " + requestId);
    }

    return toDto(request);
  }

  private ExpenseRequestDto toDto(ExpenseRequest entity) {
    return new ExpenseRequestDto(
        entity.getId(),
        entity.getUserId(),
        entity.getAmount(),
        entity.getCategory(),
        entity.getDescription(),
        entity.getExpenseDate(),
        entity.getSubmittedAt(),
        entity.getStatus(),
        entity.getAppliedPolicy() != null ? policyService.toDto(entity.getAppliedPolicy()) : null,
        entity.getDecisionRationale(),
        entity.getDecidedBy(),
        entity.getDecidedAt());
  }
}

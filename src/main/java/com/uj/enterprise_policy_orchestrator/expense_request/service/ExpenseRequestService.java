package com.uj.enterprise_policy_orchestrator.expense_request.service;

import com.uj.enterprise_policy_orchestrator.category.enums.ExpenseCategory;
import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequestHistory;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.expense_request.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.expense_request.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.policy.dto.ExpenseRequestHistoryDto;
import com.uj.enterprise_policy_orchestrator.policy.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.policy.service.PolicyService;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestHistoryRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
  private final ExpenseRequestHistoryRepository expenseRequestHistoryRepository;
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
      throw new NoApplicablePoliciesException(
          buildNoMatchingPoliciesMessage(request, dto.category()));
    }

    request.getApplicablePolicies().addAll(applicablePolicies);

    ExpenseRequest saved = expenseRequestRepository.save(request);

    recordHistory(saved.getId(), userId, null, saved.getStatus(), "Expense request created");

    return toDto(saved);
  }

  private Set<Policy> findApplicablePolicies(ExpenseRequest exp) {
    LocalDateTime expenseDateForMatching = exp.getExpenseDate();
    if (expenseDateForMatching != null
        && expenseDateForMatching.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      // Date-only input is deserialized to 00:00; match policies against the entire day.
      expenseDateForMatching = expenseDateForMatching.with(LocalTime.MAX);
    }

    return policyService.findApplicablePolicies(
        exp.getCategory(), expenseDateForMatching, exp.getAmount());
  }

  private String buildNoMatchingPoliciesMessage(
      ExpenseRequest request, String requestedCategoryRaw) {
    LocalDateTime expenseDateForMatching = request.getExpenseDate();
    if (expenseDateForMatching != null
        && expenseDateForMatching.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      expenseDateForMatching = expenseDateForMatching.with(LocalTime.MAX);
    }

    List<Policy> matchingDateAndAmount =
        policyRepository.findByDateAndAmount(expenseDateForMatching, request.getAmount());

    Set<String> availableCategories = new LinkedHashSet<>();
    matchingDateAndAmount.stream().map(Policy::getCategory).forEach(availableCategories::add);

    if (availableCategories.isEmpty()) {
      return "Decline, no matching policies";
    }

    String categoryForMessage =
        requestedCategoryRaw == null ? request.getCategory() : requestedCategoryRaw;

    return "Decline, no matching policies for category '"
        + categoryForMessage
        + "'. Available categories for the provided date and amount: "
        + availableCategories;
  }

  @Transactional(readOnly = true)
  public List<ExpenseRequestDto> getExpenseRequestHistory(String userId) {
    return expenseRequestRepository
        .findByUserId(userId, Sort.by(Sort.Direction.DESC, "submittedAt"))
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public ExpenseRequestDto cancelExpenseRequest(String userId, Long expenseRequestId) {
    ExpenseRequest request =
        expenseRequestRepository
            .findById(expenseRequestId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Expense request not found with id: " + expenseRequestId));

    if (!request.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Expense request does not belong to user: " + userId);
    }

    if (request.getStatus() != ExpenseRequestStatus.WAITING_FOR_APPROVAL) {
      throw new IllegalArgumentException(
          "Expense request cannot be cancelled with status: " + request.getStatus());
    }

    request.setStatus(ExpenseRequestStatus.CANCELLED);
    ExpenseRequest cancelled = expenseRequestRepository.save(request);

    // Record history
    recordHistory(
        cancelled.getId(),
        userId,
        ExpenseRequestStatus.WAITING_FOR_APPROVAL,
        ExpenseRequestStatus.CANCELLED,
        "Expense request cancelled by user");

    return toDto(cancelled);
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

  @Transactional(readOnly = true)
  public List<ExpenseRequestHistoryDto> getExpenseRequestStatusHistory(Long requestId) {
    return expenseRequestHistoryRepository
        .findByRequestId(requestId, Sort.by(Sort.Direction.DESC, "changedAt"))
        .stream()
        .map(this::toHistoryDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExpenseRequestHistoryDto> getUserExpenseRequestHistory(String userId) {
    return expenseRequestHistoryRepository
        .findByUserId(userId, Sort.by(Sort.Direction.DESC, "changedAt"))
        .stream()
        .map(this::toHistoryDto)
        .toList();
  }

  @Transactional
  private void recordHistory(
      Long requestId,
      String userId,
      ExpenseRequestStatus previousStatus,
      ExpenseRequestStatus newStatus,
      String changeReason) {
    ExpenseRequestHistory history =
        ExpenseRequestHistory.builder()
            .requestId(requestId)
            .userId(userId)
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .changeReason(changeReason)
            .build();
    expenseRequestHistoryRepository.save(history);
  }

  private ExpenseRequestHistoryDto toHistoryDto(ExpenseRequestHistory entity) {
    return new ExpenseRequestHistoryDto(
        entity.getId(),
        entity.getRequestId(),
        entity.getUserId(),
        entity.getPreviousStatus(),
        entity.getNewStatus(),
        entity.getChangedAt(),
        entity.getChangeReason());
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

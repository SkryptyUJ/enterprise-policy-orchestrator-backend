package com.uj.enterprise_policy_orchestrator.expense_request.service;

import com.uj.enterprise_policy_orchestrator.category.service.CategoryService;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.expense_request.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.expense_request.ExpenseRequestHistory;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.expense_request.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.policy.Policy;
import com.uj.enterprise_policy_orchestrator.policy.dto.ExpenseRequestHistoryDto;
import com.uj.enterprise_policy_orchestrator.policy.repository.PolicyRepository;
import com.uj.enterprise_policy_orchestrator.policy.service.PolicyService;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final CategoryService categoryService;

  @Transactional
  public ExpenseRequestDto createExpenseRequest(String userId, CreateExpenseRequestDto dto) {
    categoryService.getCategory(dto.categoryId());

    ExpenseRequest request =
        ExpenseRequest.builder()
            .userId(userId)
            .amount(dto.amount())
            .categoryId(dto.categoryId())
            .description(dto.description())
            .expenseDate(dto.expenseDate())
            .build();

    Set<Policy> policiesMatchingCategoryAndDate = findPoliciesMatchingCategoryAndDate(request);
    Set<Policy> applicablePolicies = findApplicablePolicies(request);
    if (applicablePolicies.isEmpty()) {
      request.setStatus(ExpenseRequestStatus.DECLINED);
      throw new NoApplicablePoliciesException(buildNoMatchingPoliciesMessage(request));
    }

    request.getApplicablePolicies().addAll(applicablePolicies);
    Set<String> conflictingPolicyNames =
        findConflictingPolicyNames(policiesMatchingCategoryAndDate, applicablePolicies);
    if (!conflictingPolicyNames.isEmpty()) {
      request.setStatus(ExpenseRequestStatus.REQUIRES_ESCALATION);
      request.getConflictingPolicyNames().addAll(conflictingPolicyNames);
    }

    ExpenseRequest saved = expenseRequestRepository.save(request);

    String creationReason =
        conflictingPolicyNames.isEmpty()
            ? "Expense request created"
            : "Expense request escalated due to policy conflict: "
                + String.join(" vs ", conflictingPolicyNames);

    recordHistory(saved.getId(), userId, null, saved.getStatus(), creationReason);

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
        exp.getCategoryId(), expenseDateForMatching, exp.getAmount());
  }

  private Set<Policy> findPoliciesMatchingCategoryAndDate(ExpenseRequest exp) {
    LocalDateTime expenseDateForMatching = exp.getExpenseDate();
    if (expenseDateForMatching != null
        && expenseDateForMatching.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      expenseDateForMatching = expenseDateForMatching.with(LocalTime.MAX);
    }

    List<Policy> policies =
        policyRepository.findByCategoryAndDate(exp.getCategoryId(), expenseDateForMatching);

    if (policies == null || policies.isEmpty()) {
      return Set.of();
    }

    Comparator<Policy> latestVersionComparator =
        Comparator.comparing(Policy::getVersion, Comparator.nullsFirst(Integer::compareTo))
            .thenComparing(Policy::getUpdatedAt, Comparator.nullsFirst(LocalDateTime::compareTo));

    Map<String, Policy> latestByPolicyId = new HashMap<>();
    for (Policy policy : policies) {
      Policy existing = latestByPolicyId.get(policy.getPolicyId());
      if (existing == null || latestVersionComparator.compare(policy, existing) > 0) {
        latestByPolicyId.put(policy.getPolicyId(), policy);
      }
    }

    return new LinkedHashSet<>(latestByPolicyId.values());
  }

  private Set<String> findConflictingPolicyNames(
      Set<Policy> policiesMatchingCategoryAndDate, Set<Policy> applicablePolicies) {
    if (policiesMatchingCategoryAndDate.isEmpty() || applicablePolicies.isEmpty()) {
      return Set.of();
    }

    Set<String> applicablePolicyIds =
        applicablePolicies.stream().map(Policy::getPolicyId).collect(Collectors.toSet());

    Optional<Policy> matchingPolicy = applicablePolicies.stream().min(this::comparePolicyDisplay);
    Optional<Policy> nonMatchingPolicy =
        policiesMatchingCategoryAndDate.stream()
            .filter(policy -> !applicablePolicyIds.contains(policy.getPolicyId()))
            .min(this::comparePolicyDisplay);

    if (matchingPolicy.isEmpty() || nonMatchingPolicy.isEmpty()) {
      return Set.of();
    }

    Set<String> conflicts = new LinkedHashSet<>();
    conflicts.add(resolvePolicyDisplayName(matchingPolicy.get()));
    conflicts.add(resolvePolicyDisplayName(nonMatchingPolicy.get()));
    return conflicts;
  }

  private int comparePolicyDisplay(Policy left, Policy right) {
    String leftDisplay = resolvePolicyDisplayName(left);
    String rightDisplay = resolvePolicyDisplayName(right);

    int byName = leftDisplay.compareToIgnoreCase(rightDisplay);
    if (byName != 0) {
      return byName;
    }

    String leftPolicyId = left.getPolicyId() == null ? "" : left.getPolicyId();
    String rightPolicyId = right.getPolicyId() == null ? "" : right.getPolicyId();
    return leftPolicyId.compareToIgnoreCase(rightPolicyId);
  }

  private String resolvePolicyDisplayName(Policy policy) {
    if (policy.getName() != null && !policy.getName().isBlank()) {
      return policy.getName();
    }
    return policy.getPolicyId() == null ? "Unknown policy" : policy.getPolicyId();
  }

  private String buildNoMatchingPoliciesMessage(
      ExpenseRequest request) {
    LocalDateTime expenseDateForMatching = request.getExpenseDate();
    if (expenseDateForMatching != null
        && expenseDateForMatching.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      expenseDateForMatching = expenseDateForMatching.with(LocalTime.MAX);
    }

    List<Policy> matchingDateAndAmount =
        policyRepository.findByDateAndAmount(expenseDateForMatching, request.getAmount());

    Set<String> availableCategories = new LinkedHashSet<>();
    matchingDateAndAmount.stream()
        .map(policy -> categoryService.getCategoryLabel(policy.getCategoryId()))
        .forEach(availableCategories::add);

    if (availableCategories.isEmpty()) {
      return "Decline, no matching policies";
    }

    String categoryForMessage = categoryService.getCategoryLabel(request.getCategoryId());

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

  @Transactional(readOnly = true)
  public List<ExpenseRequestDto> getExpenseRequestHistoryForReview() {
    return expenseRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "submittedAt")).stream()
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

    if (!isPendingReviewStatus(request.getStatus())) {
      throw new IllegalArgumentException(
          "Expense request cannot be cancelled with status: " + request.getStatus());
    }

    ExpenseRequestStatus previousStatus = request.getStatus();
    request.setStatus(ExpenseRequestStatus.CANCELLED);
    ExpenseRequest cancelled = expenseRequestRepository.save(request);

    // Record history
    recordHistory(
        cancelled.getId(),
        userId,
        previousStatus,
        ExpenseRequestStatus.CANCELLED,
        "Expense request cancelled by user");

    return toDto(cancelled);
  }

  @Transactional(readOnly = true)
  public ExpenseRequestDto getExpenseRequestById(String userId, Long requestId) {
    ExpenseRequest request = getExpenseRequestOrThrow(requestId);

    if (!request.getUserId().equals(userId)) {
      throw new jakarta.persistence.EntityNotFoundException(
          "Expense request not found with id: " + requestId);
    }

    return toDto(request);
  }

  private ExpenseRequest getExpenseRequestOrThrow(Long requestId) {
    return expenseRequestRepository
        .findById(requestId)
        .orElseThrow(
            () ->
                new jakarta.persistence.EntityNotFoundException(
                    "Expense request not found with id: " + requestId));
  }

  @Transactional(readOnly = true)
  public List<ExpenseRequestHistoryDto> getExpenseRequestStatusHistory(
      String userId, Long requestId) {
    ExpenseRequest request = getExpenseRequestOrThrow(requestId);

    if (!request.getUserId().equals(userId)) {
      throw new jakarta.persistence.EntityNotFoundException(
          "Expense request not found with id: " + requestId);
    }

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

  @Transactional(readOnly = true)
  public ExpenseRequestDto getExpenseRequestByIdForReview(Long requestId) {
    ExpenseRequest request =
        expenseRequestRepository
            .findById(requestId)
            .orElseThrow(
                () ->
                    new jakarta.persistence.EntityNotFoundException(
                        "Expense request not found with id: " + requestId));

    return toDto(request);
  }

  @Transactional
  public ExpenseRequestDto approveExpenseRequest(
      String reviewerUserId, Long expenseRequestId, String decisionRationale) {
    if (decisionRationale == null || decisionRationale.trim().isEmpty()) {
      throw new IllegalArgumentException("Decision rationale must not be empty");
    }

    ExpenseRequest request =
        expenseRequestRepository
            .findById(expenseRequestId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Expense request not found with id: " + expenseRequestId));

    if (!isPendingReviewStatus(request.getStatus())) {
      throw new IllegalArgumentException(
          "Expense request cannot be approved with status: " + request.getStatus());
    }

    ExpenseRequestStatus previousStatus = request.getStatus();

    request.setStatus(ExpenseRequestStatus.APPROVED);
    request.setDecisionRationale(decisionRationale.trim());
    request.setDecidedBy(reviewerUserId);
    request.setDecidedAt(LocalDateTime.now());

    ExpenseRequest approved = expenseRequestRepository.save(request);

    recordHistory(
        approved.getId(),
        reviewerUserId,
        previousStatus,
        ExpenseRequestStatus.APPROVED,
        "Expense request approved by reviewer");

    return toDto(approved);
  }

  @Transactional
  public ExpenseRequestDto declineExpenseRequest(
      String reviewerUserId, Long expenseRequestId, String decisionRationale) {
    if (decisionRationale == null || decisionRationale.trim().isEmpty()) {
      throw new IllegalArgumentException("Decision rationale must not be empty");
    }

    ExpenseRequest request =
        expenseRequestRepository
            .findById(expenseRequestId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Expense request not found with id: " + expenseRequestId));

    if (!isPendingReviewStatus(request.getStatus())) {
      throw new IllegalArgumentException(
          "Expense request cannot be declined with status: " + request.getStatus());
    }

    ExpenseRequestStatus previousStatus = request.getStatus();

    request.setStatus(ExpenseRequestStatus.DECLINED);
    request.setDecisionRationale(decisionRationale.trim());
    request.setDecidedBy(reviewerUserId);
    request.setDecidedAt(LocalDateTime.now());

    ExpenseRequest declined = expenseRequestRepository.save(request);

    recordHistory(
        declined.getId(),
        reviewerUserId,
        previousStatus,
        ExpenseRequestStatus.DECLINED,
        "Expense request declined by reviewer");

    return toDto(declined);
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

  private boolean isPendingReviewStatus(ExpenseRequestStatus status) {
    return status == ExpenseRequestStatus.WAITING_FOR_APPROVAL
        || status == ExpenseRequestStatus.REQUIRES_ESCALATION;
  }

  private ExpenseRequestDto toDto(ExpenseRequest entity) {
    List<String> conflictingPolicyNames =
        entity.getConflictingPolicyNames().isEmpty()
            ? null
            : entity.getConflictingPolicyNames().stream().sorted().toList();

    return new ExpenseRequestDto(
        entity.getId(),
        entity.getUserId(),
        entity.getAmount(),
        entity.getCategoryId(),
        categoryService.getCategoryLabel(entity.getCategoryId()),
        entity.getDescription(),
        entity.getExpenseDate(),
        entity.getSubmittedAt(),
        entity.getStatus(),
        entity.getAppliedPolicy() != null ? policyService.toDto(entity.getAppliedPolicy()) : null,
        conflictingPolicyNames,
        entity.getDecisionRationale(),
        entity.getDecidedBy(),
        entity.getDecidedAt());
  }
}

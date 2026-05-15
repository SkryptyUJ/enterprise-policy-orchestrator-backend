package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseCategory;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.domain.enums.ManagerDecision;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.EscalatedExpenseDecisionDto;
import com.uj.enterprise_policy_orchestrator.dto.EscalatedExpenseDecisionResultDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDetailsDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestPolicyOptionDto;
import com.uj.enterprise_policy_orchestrator.exception.ExpenseRequestNotEscalatedException;
import com.uj.enterprise_policy_orchestrator.exception.ManagerRoleRequiredException;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.exception.PolicyNotAssignedToExpenseRequestException;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
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
      throw new NoApplicablePoliciesException(
          buildNoMatchingPoliciesMessage(request, dto.category()));
    }

    request.getApplicablePolicies().addAll(applicablePolicies);
    if (applicablePolicies.size() > 1) {
      request.setStatus(ExpenseRequestStatus.ESCALATED);
    }

    ExpenseRequest saved = expenseRequestRepository.save(request);

    return toDto(saved);
  }

  List<Policy> getActivePolicies() {
    return policyRepository.findActivePolicies(LocalDateTime.now());
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
  public ExpenseRequestDetailsDto getExpenseRequestDetails(String userId, Long requestId) {
    ExpenseRequest request = getExpenseRequestForUser(userId, requestId);
    return toDetailsDto(request);
  }

  @Transactional
  public EscalatedExpenseDecisionResultDto resolveEscalatedRequest(
      String userId, Long requestId, String userRole, EscalatedExpenseDecisionDto decisionDto) {
    requireManagerRole(userRole);
    validateDecisionRequest(decisionDto);

    ExpenseRequest request = getExpenseRequestForUser(userId, requestId);
    if (request.getStatus() != ExpenseRequestStatus.ESCALATED) {
      throw new ExpenseRequestNotEscalatedException(requestId, request.getStatus());
    }

    Policy selectedPolicy =
        request.getApplicablePolicies().stream()
            .filter(policy -> policy.getId().equals(decisionDto.policyId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new PolicyNotAssignedToExpenseRequestException(
                        request.getId(), decisionDto.policyId()));

    request.setResolutionPolicy(selectedPolicy);
    request.setAppliedPolicy(selectedPolicy);
    request.setDecidedBy(userRole);
    request.setDecidedAt(LocalDateTime.now());
    request.setStatus(
        decisionDto.decision() == ManagerDecision.APPROVE
            ? ExpenseRequestStatus.APPROVED
            : ExpenseRequestStatus.DECLINED);

    ExpenseRequest saved = expenseRequestRepository.save(request);
    return toDecisionResultDto(saved);
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

    return toDto(cancelled);
  }

  @Transactional(readOnly = true)
  public ExpenseRequestDto getExpenseRequestById(String userId, Long requestId) {
    ExpenseRequest request =
      expenseRequestRepository
        .findById(requestId)
        .orElseThrow(
          () ->
            new EntityNotFoundException("Expense request not found with id: " + requestId));

    if (!request.getUserId().equals(userId)) {
      throw new EntityNotFoundException("Expense request not found with id: " + requestId);
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

  private ExpenseRequestDetailsDto toDetailsDto(ExpenseRequest entity) {
    List<ExpenseRequestPolicyOptionDto> conflictingPolicies =
        entity.getApplicablePolicies().stream()
            .map(
                policy ->
                    new ExpenseRequestPolicyOptionDto(
                        policy.getId(),
                        policy.getPolicyId(),
                        policy.getName(),
                        policy.getDescription()))
            .sorted(Comparator.comparing(ExpenseRequestPolicyOptionDto::id))
            .toList();

    Policy resolvedPolicy =
        entity.getResolutionPolicy() != null
            ? entity.getResolutionPolicy()
            : entity.getAppliedPolicy();
    Long resolutionPolicyId = resolvedPolicy != null ? resolvedPolicy.getId() : null;

    return new ExpenseRequestDetailsDto(
        entity.getId(),
        entity.getUserId(),
        entity.getAmount(),
        entity.getCategory(),
        entity.getDescription(),
        entity.getExpenseDate(),
        entity.getSubmittedAt(),
        entity.getStatus(),
        resolutionPolicyId,
        conflictingPolicies);
  }

  private EscalatedExpenseDecisionResultDto toDecisionResultDto(ExpenseRequest entity) {
    Policy selectedPolicy =
        entity.getResolutionPolicy() != null
            ? entity.getResolutionPolicy()
            : entity.getAppliedPolicy();
    Long selectedPolicyId = selectedPolicy != null ? selectedPolicy.getId() : null;
    String selectedPolicyRef = selectedPolicy != null ? selectedPolicy.getPolicyId() : null;

    return new EscalatedExpenseDecisionResultDto(
        entity.getId(), entity.getStatus(), selectedPolicyId, selectedPolicyRef);
  }

  private ExpenseRequest getExpenseRequestForUser(String userId, Long requestId) {
    return expenseRequestRepository
        .findDetailedByIdAndUserId(requestId, userId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Expense request not found for user %s and id %d"
                        .formatted(userId, requestId)));
  }

  private void requireManagerRole(String userRole) {
    if (userRole == null || !"manager".equalsIgnoreCase(userRole)) {
      throw new ManagerRoleRequiredException();
    }
  }

  private void validateDecisionRequest(EscalatedExpenseDecisionDto decisionDto) {
    if (decisionDto == null || decisionDto.policyId() == null || decisionDto.decision() == null) {
      throw new IllegalArgumentException("Decision payload requires policyId and decision");
    }
  }
}

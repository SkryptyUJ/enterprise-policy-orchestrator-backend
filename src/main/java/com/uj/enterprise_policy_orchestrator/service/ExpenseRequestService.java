package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseRequestService {

  private final ExpenseRequestRepository expenseRequestRepository;
  private final PolicyService policyService;

  @Transactional
  public ExpenseRequestDto createExpenseRequest(String userId, CreateExpenseRequestDto dto) {

    ExpenseRequest request =
        ExpenseRequest.builder()
            .userId(userId)
            .amount(dto.amount())
            .category(dto.category())
            .description(dto.description())
            .expenseDate(dto.expenseDate())
            .build();

    IO.println("EXPENSE_REQUEST: " + request.toString());

    Set<Policy> applicablePolicies = findApplicablePolicies(request);
    if (applicablePolicies.isEmpty()) {
      IO.println("NO APPLICABLE POLICIES FOUND");
      request.setStatus(ExpenseRequestStatus.DECLINED);
      throw new NoApplicablePoliciesException();
    }

    request.getApplicablePolicies().addAll(applicablePolicies);

    ExpenseRequest saved = expenseRequestRepository.save(request);
    return toDto(saved);
  }

  private Set<Policy> findApplicablePolicies(ExpenseRequest exp) {
    return policyService.findApplicablePolicies(
        exp.getCategory(), exp.getExpenseDate(), exp.getAmount());
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
        entity.getStatus());
  }
}

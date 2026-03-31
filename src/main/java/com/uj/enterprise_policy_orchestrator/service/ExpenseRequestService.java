package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseRequestService {

  private final ExpenseRequestRepository expenseRequestRepository;

  @Transactional
  public ExpenseRequestDto createExpenseRequest(Long userId, CreateExpenseRequestDto dto) {
    ExpenseRequest request =
        ExpenseRequest.builder()
            .userId(userId)
            .amount(dto.amount())
            .category(dto.category())
            .description(dto.description())
            .expenseDate(dto.expenseDate())
            .build();

    ExpenseRequest saved = expenseRequestRepository.save(request);
    return toDto(saved);
  }

  private ExpenseRequestDto toDto(ExpenseRequest entity) {
    return new ExpenseRequestDto(
        entity.getId(),
        entity.getUserId(),
        entity.getAmount(),
        entity.getCategory(),
        entity.getDescription(),
        entity.getExpenseDate(),
        entity.getSubmittedAt());
  }
}

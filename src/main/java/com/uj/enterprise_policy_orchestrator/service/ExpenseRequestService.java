package com.uj.enterprise_policy_orchestrator.service;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.User;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.repository.ExpenseRequestRepository;
import com.uj.enterprise_policy_orchestrator.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseRequestService {

  private final ExpenseRequestRepository expenseRequestRepository;
  private final UserRepository userRepository;

  @Transactional
  public ExpenseRequestDto createExpenseRequest(Long userId, CreateExpenseRequestDto dto) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    ExpenseRequest request =
        ExpenseRequest.builder()
            .user(user)
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
        entity.getUser().getId(),
        entity.getAmount(),
        entity.getCategory(),
        entity.getDescription(),
        entity.getExpenseDate(),
        entity.getSubmittedAt());
  }
}

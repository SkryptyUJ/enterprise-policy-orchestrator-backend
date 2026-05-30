package com.uj.enterprise_policy_orchestrator.repository;

import com.uj.enterprise_policy_orchestrator.expense_request.ExpenseRequestHistory;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRequestHistoryRepository
    extends JpaRepository<ExpenseRequestHistory, Long> {

  List<ExpenseRequestHistory> findByRequestId(Long requestId, Sort sort);

  List<ExpenseRequestHistory> findByUserId(String userId, Sort sort);
}

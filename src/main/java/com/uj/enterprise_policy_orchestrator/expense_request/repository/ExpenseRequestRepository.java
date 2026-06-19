package com.uj.enterprise_policy_orchestrator.expense_request.repository;

import com.uj.enterprise_policy_orchestrator.expense_request.ExpenseRequest;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRequestRepository extends JpaRepository<ExpenseRequest, Long> {
  @Query("SELECT er FROM ExpenseRequest er")
  @EntityGraph(attributePaths = "applicablePolicies")
  List<ExpenseRequest> findAllWithApplicablePolicies();

  List<ExpenseRequest> findByUserId(String userId, Sort sort);
}

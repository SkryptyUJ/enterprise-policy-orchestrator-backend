package com.uj.enterprise_policy_orchestrator.repository;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import com.uj.enterprise_policy_orchestrator.domain.Policy;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRequestRepository extends JpaRepository<ExpenseRequest, Long> {
  Set<Policy> getAllApplicablePolicies();
}

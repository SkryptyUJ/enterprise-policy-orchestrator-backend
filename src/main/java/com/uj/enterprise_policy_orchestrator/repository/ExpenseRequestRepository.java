package com.uj.enterprise_policy_orchestrator.repository;

import com.uj.enterprise_policy_orchestrator.domain.ExpenseRequest;
import java.util.List;
import java.util.Optional;
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

  //    @Override
  //    @EntityGraph(attributePaths = "applicablePolicies")
  //    List<ExpenseRequest> findAll();

  @EntityGraph(attributePaths = {"applicablePolicies", "resolutionPolicy", "appliedPolicy"})
  Optional<ExpenseRequest> findDetailedByIdAndUserId(Long id, String userId);

  List<ExpenseRequest> findByUserId(String userId);

  List<ExpenseRequest> findByUserId(String userId, Sort sort);
}

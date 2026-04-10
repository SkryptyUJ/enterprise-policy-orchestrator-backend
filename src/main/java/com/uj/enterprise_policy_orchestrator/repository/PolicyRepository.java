package com.uj.enterprise_policy_orchestrator.repository;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
  Optional<Policy> findByName(String name);

  Optional<Policy> findByVersion(Integer version);

  List<Policy> findByPolicyId(String policyId);
  @Query(
      "SELECT p FROM Policy p WHERE p.isValid = true AND p.startsAt <= :now"
          + " AND (p.expiresAt IS NULL OR p.expiresAt > :now)")
  List<Policy> findActivePolicies(@Param("now") LocalDateTime now);
}

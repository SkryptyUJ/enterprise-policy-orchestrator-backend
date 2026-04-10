package com.uj.enterprise_policy_orchestrator.repository;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
  Optional<Policy> findByName(String name);

  Optional<Policy> findByVersion(Integer version);

  List<Policy> findByPolicyId(String policyId);

  List<Policy> findByPolicyIdOrderByVersionDesc(String policyId);

  Optional<Policy> findFirstByPolicyIdOrderByVersionDesc(String policyId);
}

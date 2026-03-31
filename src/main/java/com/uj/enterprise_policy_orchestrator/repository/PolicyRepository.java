package com.uj.enterprise_policy_orchestrator.repository;

import com.uj.enterprise_policy_orchestrator.domain.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
  @Query(
      "SELECT p FROM Policy p "
          + "WHERE p.category = :category "
          + "AND p.startsAt <= :expenseDate "
          + "AND (p.expiresAt IS NULL OR p.expiresAt >= :expenseDate) "
          + "AND (p.minPrice IS NULL OR p.minPrice <= :amount) "
          + "AND (p.maxPrice IS NULL OR p.maxPrice >= :amount)")
  List<Policy> findByCategoryAndDateAndAmount(
      @Param("category") String category,
      @Param("expenseDate") LocalDate expenseDate,
      @Param("amount") BigDecimal amount);
}

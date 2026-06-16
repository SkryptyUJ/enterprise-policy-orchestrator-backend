package com.uj.enterprise_policy_orchestrator.category.repository;

import com.uj.enterprise_policy_orchestrator.category.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
  List<Category> findAllByOrderByIdAsc();

  Optional<Category> findByLabel(String label);
}

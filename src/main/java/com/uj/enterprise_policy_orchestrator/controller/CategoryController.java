package com.uj.enterprise_policy_orchestrator.controller;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseCategory;
import com.uj.enterprise_policy_orchestrator.dto.CategoryOptionDto;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  @GetMapping
  public List<CategoryOptionDto> getCategories() {
    return Arrays.stream(ExpenseCategory.values())
        .map(
            category ->
                new CategoryOptionDto(category.getId(), category.getValue(), category.getLabel()))
        .toList();
  }
}

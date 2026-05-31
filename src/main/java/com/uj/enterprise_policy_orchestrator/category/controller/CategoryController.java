package com.uj.enterprise_policy_orchestrator.category.controller;

import com.uj.enterprise_policy_orchestrator.category.dto.CategoryOptionDto;
import com.uj.enterprise_policy_orchestrator.category.enums.ExpenseCategory;
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

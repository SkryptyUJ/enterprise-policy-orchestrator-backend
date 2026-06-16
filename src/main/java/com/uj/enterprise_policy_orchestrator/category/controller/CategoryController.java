package com.uj.enterprise_policy_orchestrator.category.controller;

import com.uj.enterprise_policy_orchestrator.category.dto.CategoryOptionDto;
import com.uj.enterprise_policy_orchestrator.category.dto.CategoryUpsertDto;
import com.uj.enterprise_policy_orchestrator.category.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @GetMapping
  public List<CategoryOptionDto> getCategories() {
    return categoryService.getCategories();
  }

  @GetMapping("/{id}")
  public CategoryOptionDto getCategory(@PathVariable Integer id) {
    return categoryService.getCategory(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CategoryOptionDto createCategory(@RequestBody CategoryUpsertDto dto) {
    return categoryService.createCategory(dto);
  }

  @PutMapping("/{id}")
  public CategoryOptionDto updateCategory(
      @PathVariable Integer id, @RequestBody CategoryUpsertDto dto) {
    return categoryService.updateCategory(id, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCategory(@PathVariable Integer id) {
    categoryService.deleteCategory(id);
  }
}

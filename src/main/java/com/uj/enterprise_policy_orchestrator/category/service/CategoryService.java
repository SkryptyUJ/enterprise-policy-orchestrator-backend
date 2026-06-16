package com.uj.enterprise_policy_orchestrator.category.service;

import com.uj.enterprise_policy_orchestrator.category.Category;
import com.uj.enterprise_policy_orchestrator.category.dto.CategoryOptionDto;
import com.uj.enterprise_policy_orchestrator.category.dto.CategoryUpsertDto;
import com.uj.enterprise_policy_orchestrator.category.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;

  @Transactional(readOnly = true)
  public List<CategoryOptionDto> getCategories() {
    return categoryRepository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public CategoryOptionDto getCategory(Integer id) {
    return toDto(getCategoryOrThrow(id));
  }

  @Transactional
  public CategoryOptionDto createCategory(CategoryUpsertDto dto) {
    String label = requireText(dto.label(), "Category label must not be empty");
    validateUnique(label, null);

    Category category = Category.builder().label(label).build();
    return toDto(categoryRepository.save(category));
  }

  @Transactional
  public CategoryOptionDto updateCategory(Integer id, CategoryUpsertDto dto) {
    Category category = getCategoryOrThrow(id);
    String label = requireText(dto.label(), "Category label must not be empty");
    validateUnique(label, id);

    category.setLabel(label);
    return toDto(categoryRepository.save(category));
  }

  @Transactional
  public void deleteCategory(Integer id) {
    if (!categoryRepository.existsById(id)) {
      throw new EntityNotFoundException("Category not found with id: " + id);
    }
    categoryRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public String getCategoryLabel(Integer id) {
    if (id == null) {
      return null;
    }
    return getCategoryOrThrow(id).getLabel();
  }

  private Category getCategoryOrThrow(Integer id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
  }

  private void validateUnique(String label, Integer currentId) {
    categoryRepository
        .findByLabel(label)
        .ifPresent(
            category -> {
              if (!category.getId().equals(currentId)) {
                throw new IllegalArgumentException("Category label already exists: " + label);
              }
            });
  }

  private String requireText(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private CategoryOptionDto toDto(Category category) {
    return new CategoryOptionDto(category.getId(), category.getLabel());
  }
}

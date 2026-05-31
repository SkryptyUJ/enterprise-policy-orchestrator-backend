package com.uj.enterprise_policy_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uj.enterprise_policy_orchestrator.category.dto.CategoryOptionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@DisplayName("Category Controller E2E Tests")
class CategoryIT extends AbstractIntegrationTest {

  @Autowired private RestTemplate restTemplate;

  @Test
  @DisplayName("should return all configured expense categories")
  void shouldReturnAllConfiguredExpenseCategories() {
    ResponseEntity<CategoryOptionDto[]> response =
        restTemplate.getForEntity(baseUrl() + "/api/categories", CategoryOptionDto[].class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    CategoryOptionDto[] categories = response.getBody();
    assertNotNull(categories);
    assertEquals(4, categories.length);

    assertEquals(1, categories[0].id());
    assertEquals("1", categories[0].value());
    assertEquals("Sprzet biurowy", categories[0].label());

    assertEquals(2, categories[1].id());
    assertEquals("2", categories[1].value());
    assertEquals("Podroze sluzbowe", categories[1].label());

    assertEquals(3, categories[2].id());
    assertEquals("3", categories[2].value());
    assertEquals("Szkolenia", categories[2].label());

    assertEquals(4, categories[3].id());
    assertEquals("4", categories[3].value());
    assertEquals("Posilki", categories[3].label());

    assertTrue(java.util.Arrays.stream(categories).allMatch(category -> category.value() != null));
  }
}

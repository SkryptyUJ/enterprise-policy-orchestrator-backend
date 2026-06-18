package com.uj.enterprise_policy_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uj.enterprise_policy_orchestrator.category.dto.CategoryOptionDto;
import com.uj.enterprise_policy_orchestrator.category.dto.CategoryUpsertDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.policy.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.policy.dto.PolicyDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

@DisplayName("Category Controller E2E Tests")
class CategoryIT extends AbstractIntegrationTest {

  @Autowired private RestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute(
        "TRUNCATE TABLE expense_request_history, expense_request_policy, expense_request, policy"
            + " RESTART IDENTITY CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE category RESTART IDENTITY CASCADE");
    jdbcTemplate.update(
        """
        INSERT INTO category (id, label) VALUES
            (1, 'Sprzet biurowy'),
            (2, 'Podroze sluzbowe'),
            (3, 'Szkolenia'),
            (4, 'Posilki')
        """);
    jdbcTemplate.execute(
        "SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category))");
  }

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
    assertEquals("Sprzet biurowy", categories[0].label());

    assertEquals(2, categories[1].id());
    assertEquals("Podroze sluzbowe", categories[1].label());

    assertEquals(3, categories[2].id());
    assertEquals("Szkolenia", categories[2].label());

    assertEquals(4, categories[3].id());
    assertEquals("Posilki", categories[3].label());

    assertTrue(java.util.Arrays.stream(categories).allMatch(category -> category.label() != null));
  }

  @Test
  @DisplayName("should create, read, update, and delete category")
  void shouldCreateReadUpdateAndDeleteCategory() {
    CategoryUpsertDto createRequest = new CategoryUpsertDto("Ergonomics");

    ResponseEntity<CategoryOptionDto> createResponse =
        restTemplate.postForEntity(
            baseUrl() + "/api/categories", createRequest, CategoryOptionDto.class);

    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    assertNotNull(createResponse.getBody());
    Integer categoryId = createResponse.getBody().id();
    assertEquals("Ergonomics", createResponse.getBody().label());

    ResponseEntity<CategoryOptionDto> getResponse =
        restTemplate.getForEntity(
            baseUrl() + "/api/categories/" + categoryId, CategoryOptionDto.class);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertNotNull(getResponse.getBody());
    assertEquals("Ergonomics", getResponse.getBody().label());

    CategoryUpsertDto updateRequest = new CategoryUpsertDto("Home office");
    ResponseEntity<CategoryOptionDto> updateResponse =
        restTemplate.exchange(
            baseUrl() + "/api/categories/" + categoryId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            CategoryOptionDto.class);

    assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
    assertNotNull(updateResponse.getBody());
    assertEquals("Home office", updateResponse.getBody().label());

    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            baseUrl() + "/api/categories/" + categoryId, HttpMethod.DELETE, null, Void.class);

    assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
  }

  @Test
  @DisplayName("should use database category labels when matching expense requests to policies")
  void shouldUseDatabaseCategoryLabelsWhenMatchingExpenseRequestsToPolicies() {
    CategoryUpsertDto categoryRequest = new CategoryUpsertDto("Ergonomics");
    ResponseEntity<CategoryOptionDto> categoryResponse =
        restTemplate.postForEntity(
            baseUrl() + "/api/categories", categoryRequest, CategoryOptionDto.class);
    assertEquals(HttpStatus.CREATED, categoryResponse.getStatusCode());
    assertNotNull(categoryResponse.getBody());

    LocalDateTime startsAt = LocalDateTime.of(2026, 1, 1, 0, 0);
    LocalDateTime expenseDate = LocalDateTime.of(2026, 6, 1, 12, 0);

    CreatePolicyDto policyRequest =
        new CreatePolicyDto(
            Optional.of("CATEGORY-DB-MATCH-001"),
            categoryResponse.getBody().id(),
            "Ergonomics Policy",
            "Policy backed by a database-managed category",
            startsAt,
            null,
            new BigDecimal("100.00"),
            new BigDecimal("1000.00"),
            1);

    ResponseEntity<PolicyDto> policyResponse =
        restTemplate.postForEntity(baseUrl() + "/api/policies", policyRequest, PolicyDto.class);
    assertEquals(HttpStatus.CREATED, policyResponse.getStatusCode());
    assertNotNull(policyResponse.getBody());
    assertEquals(categoryResponse.getBody().id(), policyResponse.getBody().categoryId());
    assertEquals("Ergonomics", policyResponse.getBody().categoryLabel());

    CreateExpenseRequestDto expenseRequest =
        new CreateExpenseRequestDto(
            new BigDecimal("250.00"),
            categoryResponse.getBody().id(),
            "Standing desk converter",
            expenseDate);

    ResponseEntity<ExpenseRequestDto> expenseResponse =
        restTemplate.postForEntity(
            baseUrl() + "/api/expense-requests", expenseRequest, ExpenseRequestDto.class);

    assertEquals(HttpStatus.CREATED, expenseResponse.getStatusCode());
    assertNotNull(expenseResponse.getBody());
    assertEquals(categoryResponse.getBody().id(), expenseResponse.getBody().categoryId());
    assertEquals("Ergonomics", expenseResponse.getBody().categoryLabel());
  }
}

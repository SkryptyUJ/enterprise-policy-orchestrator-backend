package com.uj.enterprise_policy_orchestrator.expense_request.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public record CreateExpenseRequestDto(
    BigDecimal amount, Integer categoryId, String description, LocalDateTime expenseDate) {

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public CreateExpenseRequestDto(
      @JsonProperty("amount") BigDecimal amount,
      @JsonProperty("categoryId") Integer categoryId,
      @JsonProperty("description") String description,
      @JsonProperty("expenseDate") String expenseDate) {
    this(amount, categoryId, description, parseExpenseDate(expenseDate));
  }

  private static LocalDateTime parseExpenseDate(String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return null;
    }

    try {
      return LocalDateTime.parse(rawValue);
    } catch (DateTimeParseException ex) {
      try {
        return LocalDate.parse(rawValue).atStartOfDay();
      } catch (DateTimeParseException ignored) {
        throw new IllegalArgumentException(
            "Unsupported date format for expenseDate. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss");
      }
    }
  }
}

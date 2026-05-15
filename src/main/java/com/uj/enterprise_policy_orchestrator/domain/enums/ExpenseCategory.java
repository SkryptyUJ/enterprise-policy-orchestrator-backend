package com.uj.enterprise_policy_orchestrator.domain.enums;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public enum ExpenseCategory {
  OFFICE_EQUIPMENT(1, "1", "Sprzet biurowy"),
  BUSINESS_TRAVEL(2, "2", "Podroze sluzbowe"),
  TRAINING(3, "3", "Szkolenia"),
  MEALS(4, "4", "Posilki");

  private final int id;
  private final String value;
  private final String label;

  private static final Map<String, String> ALIAS_TO_VALUE = new HashMap<>();

  static {
    for (ExpenseCategory category : values()) {
      ALIAS_TO_VALUE.put(normalizeForLookup(category.value), category.value);
      ALIAS_TO_VALUE.put(normalizeForLookup(String.valueOf(category.id)), category.value);
      ALIAS_TO_VALUE.put(normalizeForLookup(category.label), category.value);
    }
  }

  ExpenseCategory(int id, String value, String label) {
    this.id = id;
    this.value = value;
    this.label = label;
  }

  public int getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  public String getLabel() {
    return label;
  }

  public static String normalize(String rawValue) {
    if (rawValue == null) {
      return null;
    }

    String trimmed = rawValue.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }

    return ALIAS_TO_VALUE.getOrDefault(normalizeForLookup(trimmed), trimmed);
  }

  private static String normalizeForLookup(String value) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    normalized = normalized.replaceAll("\\p{M}+", "");
    return normalized.toLowerCase().trim();
  }
}

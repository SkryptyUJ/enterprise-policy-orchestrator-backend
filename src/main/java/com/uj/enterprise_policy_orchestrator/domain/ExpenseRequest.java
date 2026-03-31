package com.uj.enterprise_policy_orchestrator.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 100)
  private String category;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "expense_date", nullable = false)
  private LocalDate expenseDate;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private LocalDateTime submittedAt;

  @PrePersist
  protected void onCreate() {
    submittedAt = LocalDateTime.now();
  }
}

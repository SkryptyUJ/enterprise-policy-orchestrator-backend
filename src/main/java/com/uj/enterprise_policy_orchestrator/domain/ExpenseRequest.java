package com.uj.enterprise_policy_orchestrator.domain;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
  private String userId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 100)
  private String category;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "expense_date", nullable = false)
  private LocalDateTime expenseDate;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private LocalDateTime submittedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ExpenseRequestStatus status;

  @ManyToMany
  @JoinTable(
      name = "expense_request_policy",
      joinColumns = @JoinColumn(name = "request_id"),
      inverseJoinColumns = @JoinColumn(name = "policy_id"))
  private final Set<Policy> applicablePolicies = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    submittedAt = LocalDateTime.now();
    if (status == null) {
      status = ExpenseRequestStatus.WAITING_FOR_APPROVAL;
    }
  }
}

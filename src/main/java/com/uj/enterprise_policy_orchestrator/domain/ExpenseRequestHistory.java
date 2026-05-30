package com.uj.enterprise_policy_orchestrator.domain;

import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_request_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequestHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, name = "request_id")
  private Long requestId;

  @Column(nullable = false, length = 255)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private ExpenseRequestStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ExpenseRequestStatus newStatus;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  @Column(nullable = false, length = 255)
  private String changeReason;

  @PrePersist
  protected void onCreate() {
    if (changedAt == null) {
      changedAt = LocalDateTime.now();
    }
  }
}

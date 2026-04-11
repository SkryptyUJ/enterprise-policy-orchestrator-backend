package com.uj.enterprise_policy_orchestrator.domain;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "policy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private String policyId;

  @Column(nullable = false, updatable = false)
  private String authorUserId;

  @Column(nullable = false, updatable = false)
  private Integer categoryId;

  @Column(nullable = false, updatable = false)
  private String name;

  @Column(columnDefinition = "TEXT", updatable = false)
  private String description;

  @Column(nullable = false, updatable = false)
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "starts_at", nullable = false, updatable = false)
  private LocalDateTime startsAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(updatable = false)
  private BigInteger minPrice;

  @Column(updatable = false)
  private BigInteger maxPrice;

  @Column(nullable = false)
  private Integer category;

  @Column(updatable = false)
  private Integer authorizedRole; /* @todo enum and strict definitions */

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (version == null) {
      version = 1;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}

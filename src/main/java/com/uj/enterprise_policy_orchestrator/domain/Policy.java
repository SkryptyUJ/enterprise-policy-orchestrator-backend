package com.uj.enterprise_policy_orchestrator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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

  @Column(name = "expires_at", nullable = true)
  private LocalDateTime expiresAt;

  @Column(nullable = true, updatable = false)
  private BigInteger minPrice;

  @Column(nullable = true, updatable = false)
  private BigInteger maxPrice;

  @Column(nullable = false, updatable = false)
  private Integer category;

  @Column(nullable = true, updatable = false)
  private Integer authorizedRole; /* @todo enum and strict definitions */

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (version == null) {
      version = 1;
    }
  }
}

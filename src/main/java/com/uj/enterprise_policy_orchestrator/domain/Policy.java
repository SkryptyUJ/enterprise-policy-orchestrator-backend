package com.uj.enterprise_policy_orchestrator.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
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

  @Column(nullable = false)
  private String policyId;

  @Column(nullable = false)
  private String authorUserId;

  @Column(nullable = false)
  private Integer categoryId;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalTime updatedAt;

  @Column(name = "starts_at", nullable = false)
  private LocalDate startsAt;

  @Column(name = "expires_at")
  private LocalDate expiresAt;

  @Column(nullable = true)
  private Integer minPrice;

  @Column(nullable = true)
  private Integer maxPrice;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private Integer authorizedRole; /* @todo enum and strict definitions */

  @Column(nullable = false)
  private Boolean isValid;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalTime.now();
    updatedAt = LocalTime.now();
    if (version == null) {
      version = 1;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalTime.now();
  }
}

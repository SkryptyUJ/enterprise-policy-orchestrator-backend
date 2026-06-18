package com.uj.enterprise_policy_orchestrator.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtRoleExtractor {

  private static final String ROLES_CLAIM = "roles";
  private static final String ROLES_CLAIM_SUFFIX = "/roles";

  private JwtRoleExtractor() {}

  public static Set<UserRole> extractRoles(Jwt jwt) {
    Set<UserRole> roles = new LinkedHashSet<>();
    if (jwt == null) {
      return roles;
    }

    jwt.getClaims().entrySet().stream()
        .filter(entry -> isRolesClaim(entry.getKey()))
        .map(Map.Entry::getValue)
        .forEach(value -> addRoles(roles, value));

    return roles;
  }

  public static Set<Integer> extractPolicyRoleIds(Jwt jwt) {
    Set<Integer> roleIds = new LinkedHashSet<>();
    extractRoles(jwt).stream().map(UserRole::policyRoleId).forEach(roleIds::add);
    return roleIds;
  }

  private static boolean isRolesClaim(String claimName) {
    return ROLES_CLAIM.equals(claimName) || claimName.endsWith(ROLES_CLAIM_SUFFIX);
  }

  private static void addRoles(Set<UserRole> roles, Object value) {
    if (value instanceof String role) {
      UserRole.fromAuthorityName(role).ifPresent(roles::add);
      return;
    }

    if (value instanceof Collection<?> collection) {
      collection.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .map(UserRole::fromAuthorityName)
          .flatMap(Optional::stream)
          .forEach(roles::add);
    }
  }
}

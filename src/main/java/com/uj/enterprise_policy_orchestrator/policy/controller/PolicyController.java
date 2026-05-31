package com.uj.enterprise_policy_orchestrator.policy.controller;

import com.uj.enterprise_policy_orchestrator.policy.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.policy.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.policy.dto.SetPolicyExpirationDto;
import com.uj.enterprise_policy_orchestrator.policy.service.PolicyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

  private final PolicyService policyService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PolicyDto createPolicy(
      @AuthenticationPrincipal Jwt jwt, @RequestBody CreatePolicyDto dto) {
    return policyService.createPolicy(getAuthenticatedUserId(jwt), dto);
  }

  @GetMapping("/{policyId}/history")
  @ResponseStatus(HttpStatus.OK)
  public List<PolicyDto> getPolicyHistory(@PathVariable String policyId) {
    return policyService.getPolicyHistory(policyId);
  }

  @GetMapping("/{policyId}")
  @ResponseStatus(HttpStatus.OK)
  public PolicyDto getPolicyById(@PathVariable String policyId) {
    return policyService.getPolicyByPolicyId(policyId);
  }

  @PatchMapping("/{policyId}/expiration")
  @ResponseStatus(HttpStatus.OK)
  public PolicyDto setExpiration(
      @PathVariable("policyId") Long policyId, @RequestBody SetPolicyExpirationDto dto) {
    return policyService.setExpiration(policyId, dto.expiresAt());
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public List<PolicyDto> getAllPolicies() {
    return policyService.getAllPolicies();
  }

  private String getAuthenticatedUserId(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing subject claim in JWT");
    }

    return jwt.getSubject();
  }
}

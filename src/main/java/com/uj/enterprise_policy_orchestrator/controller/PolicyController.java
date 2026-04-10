package com.uj.enterprise_policy_orchestrator.controller;

import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/policies")
@RequiredArgsConstructor
public class PolicyController {

  private final PolicyService policyService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PolicyDto createPolicy(@PathVariable String userId, @RequestBody CreatePolicyDto dto) {
    return policyService.createPolicy(userId, dto);
  }

  @GetMapping("/{policyId}")
  @ResponseStatus(HttpStatus.OK)
  public PolicyDto getPolicyById(@PathVariable Long policyId) {
    return policyService.getPolicyById(policyId);
  }
}

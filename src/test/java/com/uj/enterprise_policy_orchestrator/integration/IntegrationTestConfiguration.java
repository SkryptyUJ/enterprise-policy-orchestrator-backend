package com.uj.enterprise_policy_orchestrator.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
class IntegrationTestConfiguration {

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}

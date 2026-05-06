package com.uj.enterprise_policy_orchestrator.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class IntegrationTestConfiguration {

  @Bean
  @ServiceConnection
  @SuppressWarnings("resource")
  static PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password");
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}

package com.uj.enterprise_policy_orchestrator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  @Container
  static PostgreSQLContainer<?> db =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("test_db")
          .withUsername("test_user")
          .withPassword("test_password");

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", db::getJdbcUrl);
    registry.add("spring.datasource.username", db::getUsername);
    registry.add("spring.datasource.password", db::getPassword);
  }

  @Value("${local.server.port:8080}")
  protected int port;

  protected String baseUrl() {
    return "http://localhost:" + port;
  }
}

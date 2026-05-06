package com.uj.enterprise_policy_orchestrator.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
public abstract class AbstractIntegrationTest {
  @Value("${local.server.port:8080}")
  protected int port;

  protected String baseUrl() {
    return "http://localhost:" + port;
  }
}

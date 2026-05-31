package com.uj.enterprise_policy_orchestrator;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EnterprisePolicyOrchestratorBackendApplication {

  private static final String POLAND_TIME_ZONE = "Europe/Warsaw";

  static {
    TimeZone.setDefault(TimeZone.getTimeZone(POLAND_TIME_ZONE));
    System.setProperty("user.timezone", POLAND_TIME_ZONE);
  }

  static void main(String[] args) {
    SpringApplication.run(EnterprisePolicyOrchestratorBackendApplication.class, args);
  }
}

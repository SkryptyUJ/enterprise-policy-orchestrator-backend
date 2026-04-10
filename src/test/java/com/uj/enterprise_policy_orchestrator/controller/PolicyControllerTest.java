package com.uj.enterprise_policy_orchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uj.enterprise_policy_orchestrator.dto.CreatePolicyDto;
import com.uj.enterprise_policy_orchestrator.dto.PolicyDto;
import com.uj.enterprise_policy_orchestrator.service.PolicyService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyController")
class PolicyControllerTest {

  private MockMvc mockMvc;

  @Mock private PolicyService policyService;

  @InjectMocks private PolicyController policyController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(policyController).build();
  }

  @Nested
  @DisplayName("POST /api/users/{userId}/policies")
  class CreatePolicyEndpoint {

    @Test
    @DisplayName("should return 201 CREATED with the new policy data")
    void shouldReturn201WithCreatedPolicy() throws Exception {
      String userId = "1";
      LocalDate startsAt = LocalDate.of(2026, 4, 1);
      LocalDate expiresAt = LocalDate.of(2027, 3, 31);

      PolicyDto responseDto =
          new PolicyDto(
              100L,
              "100",
              userId,
              1,
              "Travel Policy",
              "Company travel policy",
              1,
              LocalTime.now(),
              LocalTime.now(),
              startsAt,
              expiresAt,
              100,
              5000,
              "Travels",
              2,
              true);

      when(policyService.createPolicy(eq(userId), any(CreatePolicyDto.class)))
          .thenReturn(responseDto);

      String requestJson =
          """
          {
            "policyId": "100",
            "categoryId": 1,
            "name": "Travel Policy",
            "description": "Company travel policy",
            "startsAt": "2026-04-01",
            "expiresAt": "2027-03-31",
            "minPrice": 100,
            "maxPrice": 5000,
            "category": "Travels",
            "authorizedRole": 2
          }
          """;

      mockMvc
          .perform(
              post("/api/users/{userId}/policies", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(100))
          .andExpect(jsonPath("$.authorUserId").value("1"))
          .andExpect(jsonPath("$.name").value("Travel Policy"))
          .andExpect(jsonPath("$.isValid").value(true));
    }

    @Test
    @DisplayName("should delegate to service with correct parameters")
    void shouldDelegateToServiceWithCorrectParameters() throws Exception {
      String userId = "5";

      PolicyDto responseDto =
          new PolicyDto(
              50L,
              "200",
              userId,
              2,
              "Hardware Policy",
              "Equipment policy",
              1,
              LocalTime.now(),
              LocalTime.now(),
              LocalDate.of(2026, 5, 1),
              null,
              500,
              10000,
              "Equipment",
              3,
              true);

      when(policyService.createPolicy(eq(userId), any(CreatePolicyDto.class)))
          .thenReturn(responseDto);

      String requestJson =
          """
          {
            "policyId": "200",
            "categoryId": 2,
            "name": "Hardware Policy",
            "description": "Equipment policy",
            "startsAt": "2026-05-01",
            "expiresAt": null,
            "minPrice": 500,
            "maxPrice": 10000,
            "category": "Equipment",
            "authorizedRole": 3
          }
          """;

      mockMvc
          .perform(
              post("/api/users/{userId}/policies", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.authorUserId").value("5"))
          .andExpect(jsonPath("$.policyId").value("200"))
          .andExpect(jsonPath("$.category").value("Equipment"));
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/policies/{policyId}")
  class GetPolicyEndpoint {

    @Test
    @DisplayName("should return 200 OK with policy data")
    void shouldReturn200WithPolicyData() throws Exception {
      Long policyId = 1L;
      LocalDate nowDate = LocalDate.now();

      PolicyDto responseDto =
          new PolicyDto(
              policyId,
              "100",
              "2",
              1,
              "Test Policy",
              "Test Description",
              1,
              LocalTime.now(),
              LocalTime.now(),
              nowDate.plusDays(1),
              nowDate.plusYears(1),
              100,
              5000,
              "TestCategory",
              2,
              true);

      when(policyService.getPolicyById(policyId)).thenReturn(responseDto);

      mockMvc
          .perform(get("/api/users/{userId}/policies/{policyId}", "2", policyId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(policyId))
          .andExpect(jsonPath("$.name").value("Test Policy"))
          .andExpect(jsonPath("$.isValid").value(true));
    }
  }
}

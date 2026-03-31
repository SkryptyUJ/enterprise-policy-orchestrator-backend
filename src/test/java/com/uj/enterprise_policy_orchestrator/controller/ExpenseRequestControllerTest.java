package com.uj.enterprise_policy_orchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.exception.NoApplicablePoliciesException;
import com.uj.enterprise_policy_orchestrator.service.ExpenseRequestService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@DisplayName("ExpenseRequestController")
class ExpenseRequestControllerTest {

  private MockMvc mockMvc;

  @Mock private ExpenseRequestService expenseRequestService;
  @InjectMocks private ExpenseRequestController expenseRequestController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(expenseRequestController).build();
  }

  @Nested
  @DisplayName("POST /api/users/{userId}/expense-requests")
  class CreateExpenseRequestEndpoint {

    @Test
    @DisplayName("should return 201 CREATED with the new expense request data")
    void shouldReturn201WithCreatedExpenseRequest() throws Exception {
      // given
      Long userId = 1L;
      LocalDateTime submittedAt = LocalDateTime.of(2026, 3, 23, 10, 30, 0);

      ExpenseRequestDto responseDto =
          new ExpenseRequestDto(
              100L,
              userId,
              new BigDecimal("1500.00"),
              "Business travel",
              "Business trip to Krakow – train tickets and hotel",
              LocalDate.of(2026, 3, 20),
              submittedAt,
              ExpenseRequestStatus.WAITING_FOR_APPROVAL);

      when(expenseRequestService.createExpenseRequest(
              eq(userId), any(CreateExpenseRequestDto.class)))
          .thenReturn(responseDto);

      String requestJson =
          """
          {
            "amount": 1500.00,
            "category": "Business travel",
            "description": "Business trip to Krakow – train tickets and hotel",
            "expenseDate": "2026-03-20"
          }
          """;

      // when & then
      mockMvc
          .perform(
              post("/api/users/{userId}/expense-requests", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(100))
          .andExpect(jsonPath("$.userId").value(1))
          .andExpect(jsonPath("$.amount").value(1500.00))
          .andExpect(jsonPath("$.category").value("Business travel"))
          .andExpect(
              jsonPath("$.description").value("Business trip to Krakow – train tickets and hotel"))
          .andExpect(jsonPath("$.expenseDate").value("2026-03-20"))
          .andExpect(jsonPath("$.submittedAt").exists())
          .andExpect(jsonPath("$.status").value("WAITING_FOR_APPROVAL"));
    }

    @Test
    @DisplayName("should delegate to service with correct parameters")
    void shouldDelegateToServiceWithCorrectParameters() throws Exception {
      // given
      Long userId = 5L;

      ExpenseRequestDto responseDto =
          new ExpenseRequestDto(
              1L,
              userId,
              new BigDecimal("42.50"),
              "Office supplies",
              "Pens",
              LocalDate.of(2026, 6, 15),
              LocalDateTime.now(),
              ExpenseRequestStatus.WAITING_FOR_APPROVAL);

      when(expenseRequestService.createExpenseRequest(
              eq(userId), any(CreateExpenseRequestDto.class)))
          .thenReturn(responseDto);

      String requestJson =
          """
          {
            "amount": 42.50,
            "category": "Office supplies",
            "description": "Pens",
            "expenseDate": "2026-06-15"
          }
          """;

      // when & then
      mockMvc
          .perform(
              post("/api/users/{userId}/expense-requests", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.userId").value(5))
          .andExpect(jsonPath("$.amount").value(42.50))
          .andExpect(jsonPath("$.category").value("Office supplies"))
          .andExpect(jsonPath("$.status").value("WAITING_FOR_APPROVAL"));
    }

    @Test
    @DisplayName("should return 400 when no applicable policies exist")
    void shouldReturn400WhenNoApplicablePoliciesExist() throws Exception {
      Long userId = 7L;

      when(expenseRequestService.createExpenseRequest(
              eq(userId), any(CreateExpenseRequestDto.class)))
          .thenThrow(new NoApplicablePoliciesException());

      String requestJson =
          """
          {
            "amount": 10.00,
            "category": "Snacks",
            "description": "Coffee",
            "expenseDate": "2026-02-01"
          }
          """;

      mockMvc
          .perform(
              post("/api/users/{userId}/expense-requests", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isBadRequest())
          .andExpect(status().reason("Decline, no matching policies"));
    }
  }
}

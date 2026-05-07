package com.uj.enterprise_policy_orchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.service.ExpenseRequestService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
      String userId = "user-123";
      LocalDateTime submittedAt = LocalDateTime.of(2026, 3, 23, 10, 30, 0);

      ExpenseRequestDto responseDto =
          new ExpenseRequestDto(
              100L,
              userId,
              new BigDecimal("1500.00"),
              "Business travel",
              "Business trip to Krakow – train tickets and hotel",
              LocalDateTime.of(2026, 3, 20, 0, 0, 0),
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
            "expenseDate": "2026-03-20T00:00:00"
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
          .andExpect(jsonPath("$.userId").value(userId))
          .andExpect(jsonPath("$.amount").value(1500.00))
          .andExpect(jsonPath("$.category").value("Business travel"))
          .andExpect(
              jsonPath("$.description").value("Business trip to Krakow – train tickets and hotel"))
          .andExpect(jsonPath("$.expenseDate").value("2026-03-20T00:00:00"))
          .andExpect(jsonPath("$.submittedAt").exists())
          .andExpect(jsonPath("$.status").value("WAITING_FOR_APPROVAL"));
    }

    @Test
    @DisplayName("should delegate to service with correct parameters")
    void shouldDelegateToServiceWithCorrectParameters() throws Exception {
      // given
      String userId = "user-456";

      ExpenseRequestDto responseDto =
          new ExpenseRequestDto(
              1L,
              userId,
              new BigDecimal("42.50"),
              "Office supplies",
              "Pens",
              LocalDateTime.of(2026, 6, 15, 0, 0, 0),
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
            "expenseDate": "2026-06-15T00:00:00"
          }
          """;

      // when & then
      mockMvc
          .perform(
              post("/api/users/{userId}/expense-requests", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.userId").value(userId))
          .andExpect(jsonPath("$.amount").value(42.50))
          .andExpect(jsonPath("$.category").value("Office supplies"));
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests")
  class GetExpenseRequestHistoryEndpoint {

    @Test
    @DisplayName("should return 200 OK with list of all expense requests sorted by submission date")
    void shouldReturn200WithExpenseRequestHistory() throws Exception {
      // given
      Long userId = 2L;

      List<ExpenseRequestDto> historyDtos =
          List.of(
              new ExpenseRequestDto(
                  103L,
                  "user-2",
                  new BigDecimal("75.50"),
                  "Office supplies",
                  "Notebooks and pens",
                  LocalDateTime.of(2026, 2, 1, 9, 15, 0),
                  LocalDateTime.of(2026, 2, 2, 9, 15, 0),
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL),
              new ExpenseRequestDto(
                  102L,
                  "user-2",
                  new BigDecimal("150.00"),
                  "Meals",
                  "Team lunch",
                  LocalDateTime.of(2026, 1, 20, 9, 15, 0),
                  LocalDateTime.of(2026, 1, 21, 14, 30, 0),
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL),
              new ExpenseRequestDto(
                  101L,
                  "user-2",
                  new BigDecimal("500.00"),
                  "Travel",
                  "Flight to conference",
                  LocalDateTime.of(2026, 1, 15, 9, 15, 0),
                  LocalDateTime.of(2026, 1, 16, 10, 0, 0),
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL));

      when(expenseRequestService.getExpenseRequestHistory(userId)).thenReturn(historyDtos);

      // when & then
      mockMvc
          .perform(get("/api/users/{userId}/expense-requests", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(3))
          .andExpect(jsonPath("$[0].id").value(103))
          .andExpect(jsonPath("$[0].amount").value(75.50))
          .andExpect(jsonPath("$[1].id").value(102))
          .andExpect(jsonPath("$[1].category").value("Meals"))
          .andExpect(jsonPath("$[2].id").value(101))
          .andExpect(jsonPath("$[2].description").value("Flight to conference"));
    }

    @Test
    @DisplayName("should return 200 OK with empty list when user has no requests")
    void shouldReturn200WithEmptyListWhenNoRequests() throws Exception {
      // given
      Long userId = 5L;

      when(expenseRequestService.getExpenseRequestHistory(userId)).thenReturn(List.of());

      // when & then
      mockMvc
          .perform(get("/api/users/{userId}/expense-requests", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }
}

package com.uj.enterprise_policy_orchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uj.enterprise_policy_orchestrator.domain.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.dto.ExpenseRequestHistoryDto;
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
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              null,
              null,
              null,
              null);

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
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              null,
              null,
              null,
              null);

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

    @Test
    @DisplayName("should accept expenseDate in yyyy-MM-dd format")
    void shouldAcceptDateOnlyExpenseDateFormat() throws Exception {
      // given
      String userId = "user-789";

      ExpenseRequestDto responseDto =
          new ExpenseRequestDto(
              2L,
              userId,
              new BigDecimal("100.00"),
              "Sprzęt biurowy",
              "f",
              LocalDateTime.of(2026, 5, 13, 0, 0, 0),
              LocalDateTime.now(),
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              null,
              null,
              null,
              null);

      when(expenseRequestService.createExpenseRequest(
              eq(userId), any(CreateExpenseRequestDto.class)))
          .thenReturn(responseDto);

      String requestJson =
          """
            {
            "amount": 100.00,
            "category": "Sprzęt biurowy",
            "description": "f",
            "expenseDate": "2026-05-13"
            }
            """;

      // when & then
      mockMvc
          .perform(
              post("/api/users/{userId}/expense-requests", userId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.expenseDate").value("2026-05-13T00:00:00"));
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests")
  class GetExpenseRequestHistoryEndpoint {

    @Test
    @DisplayName("should return 200 OK with list of all expense requests sorted by submission date")
    void shouldReturn200WithExpenseRequestHistory() throws Exception {
      // given
      String userId = "user-2";

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
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL,
                  null,
                  null,
                  null,
                  null),
              new ExpenseRequestDto(
                  102L,
                  "user-2",
                  new BigDecimal("150.00"),
                  "Meals",
                  "Team lunch",
                  LocalDateTime.of(2026, 1, 20, 9, 15, 0),
                  LocalDateTime.of(2026, 1, 21, 14, 30, 0),
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL,
                  null,
                  null,
                  null,
                  null),
              new ExpenseRequestDto(
                  101L,
                  "user-2",
                  new BigDecimal("500.00"),
                  "Travel",
                  "Flight to conference",
                  LocalDateTime.of(2026, 1, 15, 9, 15, 0),
                  LocalDateTime.of(2026, 1, 16, 10, 0, 0),
                  ExpenseRequestStatus.WAITING_FOR_APPROVAL,
                  null,
                  null,
                  null,
                  null));

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
      String userId = "user-5";

      when(expenseRequestService.getExpenseRequestHistory(userId)).thenReturn(List.of());

      // when & then
      mockMvc
          .perform(get("/api/users/{userId}/expense-requests", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }
  }

  @Nested
  @DisplayName("DELETE /api/users/{userId}/expense-requests/{expenseRequestId}")
  class CancelExpenseRequestEndpoint {

    @Test
    @DisplayName("should return 200 OK with cancelled expense request")
    void shouldReturn200WithCancelledExpenseRequest() throws Exception {
      // given
      String userId = "user-123";
      Long expenseRequestId = 100L;
      LocalDateTime submittedAt = LocalDateTime.of(2026, 3, 23, 10, 30, 0);

      ExpenseRequestDto cancelledDto =
          new ExpenseRequestDto(
              expenseRequestId,
              userId,
              new BigDecimal("1500.00"),
              "Business travel",
              "Business trip to Krakow – train tickets and hotel",
              LocalDateTime.of(2026, 3, 20, 0, 0, 0),
              submittedAt,
              ExpenseRequestStatus.CANCELLED,
              null,
              null,
              null,
              null);

      when(expenseRequestService.cancelExpenseRequest(userId, expenseRequestId))
          .thenReturn(cancelledDto);

      // when & then
      mockMvc
          .perform(
              delete(
                  "/api/users/{userId}/expense-requests/{expenseRequestId}",
                  userId,
                  expenseRequestId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(expenseRequestId))
          .andExpect(jsonPath("$.userId").value(userId))
          .andExpect(jsonPath("$.status").value("CANCELLED"))
          .andExpect(jsonPath("$.amount").value(1500.00))
          .andExpect(jsonPath("$.category").value("Business travel"));
    }

    @Test
    @DisplayName("should delegate to service with correct parameters")
    void shouldDelegateToServiceWithCorrectParameters() throws Exception {
      // given
      String userId = "user-456";
      Long expenseRequestId = 42L;

      ExpenseRequestDto cancelledDto =
          new ExpenseRequestDto(
              expenseRequestId,
              userId,
              new BigDecimal("250.00"),
              "Office supplies",
              "Pens and notebooks",
              LocalDateTime.of(2026, 6, 15, 0, 0, 0),
              LocalDateTime.now(),
              ExpenseRequestStatus.CANCELLED,
              null,
              null,
              null,
              null);

      when(expenseRequestService.cancelExpenseRequest(eq(userId), eq(expenseRequestId)))
          .thenReturn(cancelledDto);

      // when & then
      mockMvc
          .perform(
              delete(
                  "/api/users/{userId}/expense-requests/{expenseRequestId}",
                  userId,
                  expenseRequestId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(expenseRequestId))
          .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests/{expenseRequestId}/history")
  class GetExpenseRequestStatusHistoryEndpoint {

    @Test
    @DisplayName("should return 200 OK with the status history for the expense request")
    void shouldReturnStatusHistory() throws Exception {
      // given
      String userId = "user123";
      Long expenseRequestId = 1L;

      LocalDateTime now = LocalDateTime.now();
      ExpenseRequestHistoryDto history1 =
          new ExpenseRequestHistoryDto(
              1L,
              expenseRequestId,
              userId,
              null,
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              now.minusDays(2),
              "Expense request created");

      ExpenseRequestHistoryDto history2 =
          new ExpenseRequestHistoryDto(
              2L,
              expenseRequestId,
              userId,
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              ExpenseRequestStatus.CANCELLED,
              now.minusHours(1),
              "Expense request cancelled by user");

      when(expenseRequestService.getExpenseRequestStatusHistory(expenseRequestId))
          .thenReturn(List.of(history2, history1));

      // when & then
      mockMvc
          .perform(
              get(
                  "/api/users/{userId}/expense-requests/{expenseRequestId}/history",
                  userId,
                  expenseRequestId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
          .andExpect(jsonPath("$[0].newStatus").value("CANCELLED"))
          .andExpect(jsonPath("$[1].newStatus").value("WAITING_FOR_APPROVAL"));
    }

    @Test
    @DisplayName("should return 200 OK with empty list when no history exists")
    void shouldReturnEmptyHistoryList() throws Exception {
      // given
      String userId = "user123";
      Long expenseRequestId = 999L;

      when(expenseRequestService.getExpenseRequestStatusHistory(expenseRequestId))
          .thenReturn(List.of());

      // when & then
      mockMvc
          .perform(
              get(
                  "/api/users/{userId}/expense-requests/{expenseRequestId}/history",
                  userId,
                  expenseRequestId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }
  }

  @Nested
  @DisplayName("GET /api/users/{userId}/expense-requests/history/all")
  class GetUserExpenseRequestHistoryEndpoint {

    @Test
    @DisplayName("should return 200 OK with all status history for the user")
    void shouldReturnAllUserHistory() throws Exception {
      // given
      String userId = "user123";
      LocalDateTime now = LocalDateTime.now();

      ExpenseRequestHistoryDto history1 =
          new ExpenseRequestHistoryDto(
              1L,
              1L,
              userId,
              null,
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              now.minusDays(2),
              "Expense request created");

      ExpenseRequestHistoryDto history2 =
          new ExpenseRequestHistoryDto(
              2L,
              1L,
              userId,
              ExpenseRequestStatus.WAITING_FOR_APPROVAL,
              ExpenseRequestStatus.CANCELLED,
              now.minusHours(1),
              "Expense request cancelled by user");

      when(expenseRequestService.getUserExpenseRequestHistory(userId))
          .thenReturn(List.of(history2, history1));

      // when & then
      mockMvc
          .perform(get("/api/users/{userId}/expense-requests/history/all", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
          .andExpect(
              jsonPath(
                  "$[*].userId",
                  org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo(userId))));
    }

    @Test
    @DisplayName("should return 200 OK with empty list when user has no history")
    void shouldReturnEmptyHistoryListForUser() throws Exception {
      // given
      String userId = "userWithNoHistory";

      when(expenseRequestService.getUserExpenseRequestHistory(userId)).thenReturn(List.of());

      // when & then
      mockMvc
          .perform(get("/api/users/{userId}/expense-requests/history/all", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }
  }
}

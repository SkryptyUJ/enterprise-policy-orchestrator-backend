package com.uj.enterprise_policy_orchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uj.enterprise_policy_orchestrator.expense_request.controller.ExpenseRequestController;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.CreateExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.dto.ExpenseRequestDto;
import com.uj.enterprise_policy_orchestrator.expense_request.enums.ExpenseRequestStatus;
import com.uj.enterprise_policy_orchestrator.expense_request.service.ExpenseRequestService;
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
    @DisplayName("GET /api/users/{userId}/expense-requests/review")
    class GetExpenseRequestHistoryForReviewEndpoint {

        @Test
        @DisplayName("should return 200 OK with all requests for review")
        void shouldReturn200WithAllRequestsForReview() throws Exception {
            String reviewerId = "manager-1";

            List<ExpenseRequestDto> reviewDtos =
                    List.of(
                            new ExpenseRequestDto(
                                    201L,
                                    "employee-1",
                                    new BigDecimal("300.00"),
                                    "Travel",
                                    "Taxi",
                                    LocalDateTime.of(2026, 5, 2, 0, 0, 0),
                                    LocalDateTime.of(2026, 5, 3, 12, 0, 0),
                                    ExpenseRequestStatus.WAITING_FOR_APPROVAL,
                                    null,
                                    null,
                                    null,
                                    null),
                            new ExpenseRequestDto(
                                    202L,
                                    "employee-2",
                                    new BigDecimal("120.00"),
                                    "Meals",
                                    "Lunch",
                                    LocalDateTime.of(2026, 5, 1, 0, 0, 0),
                                    LocalDateTime.of(2026, 5, 2, 10, 0, 0),
                                    ExpenseRequestStatus.APPROVED,
                                    null,
                                    "Approved",
                                    "manager-1",
                                    LocalDateTime.of(2026, 5, 2, 11, 0, 0)));

            when(expenseRequestService.getExpenseRequestHistoryForReview()).thenReturn(reviewDtos);

            mockMvc
                    .perform(get("/api/users/{userId}/expense-requests/review", reviewerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].userId").value("employee-1"))
                    .andExpect(jsonPath("$[1].userId").value("employee-2"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{userId}/expense-requests/review/{requestId}/approve")
    class ApproveExpenseRequestEndpoint {

        @Test
        @DisplayName("should return 200 OK with approved expense request")
        void shouldReturn200WithApprovedExpenseRequest() throws Exception {
            String reviewerId = "manager-1";
            Long requestId = 301L;
            LocalDateTime decidedAt = LocalDateTime.of(2026, 6, 1, 9, 30, 0);

            ExpenseRequestDto approvedDto =
                    new ExpenseRequestDto(
                            requestId,
                            "employee-1",
                            new BigDecimal("450.00"),
                            "Travel",
                            "Hotel",
                            LocalDateTime.of(2026, 5, 25, 0, 0, 0),
                            LocalDateTime.of(2026, 5, 26, 8, 0, 0),
                            ExpenseRequestStatus.APPROVED,
                            null,
                            "Wydatek jest zgodny z obowiązującą polityką.",
                            reviewerId,
                            decidedAt);

            when(expenseRequestService.approveExpenseRequest(eq(reviewerId), eq(requestId), any()))
                    .thenReturn(approvedDto);

            String requestJson =
                    """
                    {
                        "decisionRationale": "Wydatek jest zgodny z obowiązującą polityką."
                    }
                    """;

            mockMvc
                    .perform(
                            patch("/api/users/{userId}/expense-requests/review/{requestId}/approve", reviewerId, requestId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(301))
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.decidedBy").value("manager-1"))
                    .andExpect(
                            jsonPath("$.decisionRationale")
                                    .value("Wydatek jest zgodny z obowiązującą polityką."));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{userId}/expense-requests/review/{requestId}/decline")
    class DeclineExpenseRequestEndpoint {

        @Test
        @DisplayName("should return 200 OK with declined expense request")
        void shouldReturn200WithDeclinedExpenseRequest() throws Exception {
            String reviewerId = "manager-2";
            Long requestId = 302L;
            LocalDateTime decidedAt = LocalDateTime.of(2026, 6, 2, 10, 0, 0);

            ExpenseRequestDto declinedDto =
                    new ExpenseRequestDto(
                            requestId,
                            "employee-2",
                            new BigDecimal("125.00"),
                            "Meals",
                            "Dinner",
                            LocalDateTime.of(2026, 5, 25, 0, 0, 0),
                            LocalDateTime.of(2026, 5, 26, 8, 0, 0),
                            ExpenseRequestStatus.DECLINED,
                            null,
                            "Wydatek poza limitem polityki.",
                            reviewerId,
                            decidedAt);

            when(expenseRequestService.declineExpenseRequest(eq(reviewerId), eq(requestId), any()))
                    .thenReturn(declinedDto);

            String requestJson =
                    """
                    {
                        "decisionRationale": "Wydatek poza limitem polityki."
                    }
                    """;

            mockMvc
                    .perform(
                            patch("/api/users/{userId}/expense-requests/review/{requestId}/decline", reviewerId, requestId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(302))
                    .andExpect(jsonPath("$.status").value("DECLINED"))
                    .andExpect(jsonPath("$.decidedBy").value("manager-2"))
                    .andExpect(jsonPath("$.decisionRationale").value("Wydatek poza limitem polityki."));

                    verify(expenseRequestService).declineExpenseRequest(reviewerId, requestId, "Wydatek poza limitem polityki.");
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
}

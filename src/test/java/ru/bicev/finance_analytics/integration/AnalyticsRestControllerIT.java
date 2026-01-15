package ru.bicev.finance_analytics.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.util.CategoryType;
import ru.bicev.finance_analytics.util.TestUtil;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AnalyticsRestControllerIT {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepository;

        @Autowired
        CategoryRepository categoryRepository;

        @Autowired
        BudgetRepository budgetRepository;

        @Autowired
        TransactionRepository transactionRepository;

        @Autowired
        RecurringTransactionRepository recurringTransactionRepository;

        private User user;
        private Category cat1;
        private Category cat2;
        private Budget budget1;
        private Budget budget2;
        private Transaction tr1;
        private Transaction tr2;
        private Transaction tr3;
        private Transaction tr4;
        private Transaction tr5;
        private RecurringTransaction rtr1;
        private RecurringTransaction rtr2;
        private RecurringTransaction rtr3;
        private RecurringTransaction rtr4;

        private static final LocalDateTime NOW = LocalDateTime.of(2025, 12, 25, 10, 0);
        public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.yyyy");

        private String dateRange = """
                        {
                            "start": "2025-06-01",
                            "end": "2025-12-25"
                        }
                        """;
        private String invalidDateRange = """
                        {
                            "start": "abbd",
                            "end": ""
                        }
                        """;

        @BeforeEach
        void setUp() {
                user = userRepository.save(
                                User.builder()
                                                .createdAt(NOW)
                                                .email("test@email.com")
                                                .lastLoginAt(NOW)
                                                .avatarUrl("avatar.png")
                                                .name("John Doe")
                                                .provider("google")
                                                .providerId("test-sub")
                                                .build());
                cat1 = categoryRepository.save(TestUtil.generateCategory(user, CategoryType.EXPENSE, "Category 1"));
                cat2 = categoryRepository.save(TestUtil.generateCategory(user, CategoryType.EXPENSE, "Category 2"));
                budget1 = budgetRepository
                                .save(TestUtil.generateBudget(user, cat1, BigDecimal.valueOf(10000.00),
                                                YearMonth.of(2025, 12)));
                budget2 = budgetRepository
                                .save(TestUtil.generateBudget(user, cat2, BigDecimal.valueOf(5000.00),
                                                YearMonth.of(2025, 12)));
                tr1 = transactionRepository
                                .save(TestUtil.generateTransaction(user, cat1, BigDecimal.valueOf(2000.00), 2025, 10,
                                                15));
                tr2 = transactionRepository
                                .save(TestUtil.generateTransaction(user, cat1, BigDecimal.valueOf(1500.00), 2025, 11,
                                                15));
                tr3 = transactionRepository
                                .save(TestUtil.generateTransaction(user, cat1, BigDecimal.valueOf(3125.50), 2025, 12,
                                                01));
                tr4 = transactionRepository
                                .save(TestUtil.generateTransaction(user, cat2, BigDecimal.valueOf(2500.00), 2025, 12,
                                                15));
                tr5 = transactionRepository
                                .save(TestUtil.generateTransaction(user, cat2, BigDecimal.valueOf(3900.00), 2025, 12,
                                                19));
                rtr1 = recurringTransactionRepository
                                .save(TestUtil.generateRecurringTransaction(user, cat1, BigDecimal.valueOf(10.99), 2030,
                                                1, 5));
                rtr2 = recurringTransactionRepository
                                .save(TestUtil.generateRecurringTransaction(user, cat1, BigDecimal.valueOf(15.99), 2030,
                                                1, 9));
                rtr3 = recurringTransactionRepository
                                .save(TestUtil.generateRecurringTransaction(user, cat2, BigDecimal.valueOf(20.99), 2030,
                                                1, 16));
                rtr4 = recurringTransactionRepository
                                .save(TestUtil.generateRecurringTransaction(user, cat2, BigDecimal.valueOf(25.99), 2030,
                                                2, 15));
        }

        // ---------------------
        // getExpensesByCategory()
        // ---------------------
        @Test
        void getExpensesByCategory_success() throws Exception {
                mockMvc.perform(get("/api/analytics/categories")
                                .param("month", "2025-12")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].category").value(cat2.getName()))
                                .andExpect(jsonPath("$[0].total").value(tr4.getAmount().add(tr5.getAmount())))
                                .andExpect(jsonPath("$[1].category").value(cat1.getName()))
                                .andExpect(jsonPath("$[1].total").value(tr3.getAmount()));
        }

        @Test
        void getExpensesByCategory_invalidParam() throws Exception {
                mockMvc.perform(get("/api/analytics/categories")
                                .param("month", "ab--15")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        // ---------------------
        // getTopCategories()
        // ---------------------
        @Test
        void getTopCategories_success() throws Exception {
                mockMvc.perform(get("/api/analytics/categories/top")
                                .param("month", "2025-12")
                                .param("limit", "1")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].category").value(cat2.getName()))
                                .andExpect(jsonPath("$[0].total").value(tr4.getAmount().add(tr5.getAmount())));
        }

        @Test
        void getTopCategories_withInvalidParam() throws Exception {
                mockMvc.perform(get("/api/analytics/categories/top")
                                .param("month", "2025-12")
                                .param("limit", "-700")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isBadRequest());
        }

        // ---------------------
        // getDailyExpenses()
        // ---------------------
        @Test
        void getDailyExpenses_success() throws Exception {
                mockMvc.perform(get("/api/analytics/daily")
                                .param("month", "2025-12")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].date").value(tr3.getDate().toString()))
                                .andExpect(jsonPath("$[0].amount").value(tr3.getAmount()))
                                .andExpect(jsonPath("$[1].date").value(tr4.getDate().toString()))
                                .andExpect(jsonPath("$[1].amount").value(tr4.getAmount()))
                                .andExpect(jsonPath("$[2].date").value(tr5.getDate().toString()))
                                .andExpect(jsonPath("$[2].amount").value(tr5.getAmount()));
        }

        @Test
        void getDailyExpenses_withInvalidParam() throws Exception {
                mockMvc.perform(get("/api/analytics/daily")
                                .param("month", "17-1452")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        // ---------------------
        // getMonthlyExpenses()
        // ---------------------
        @Test
        void getMonthlyExpenses_success() throws Exception {
                BigDecimal decemberTotal = tr3.getAmount().add(tr4.getAmount()).add(tr5.getAmount());
                mockMvc.perform(post("/api/analytics/monthly")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(dateRange)
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].month").value(tr1.getDate().format(FORMAT)))
                                .andExpect(jsonPath("$[0].total").value(tr1.getAmount()))
                                .andExpect(jsonPath("$[1].month").value(tr2.getDate().format(FORMAT)))
                                .andExpect(jsonPath("$[1].total").value(tr2.getAmount()))
                                .andExpect(jsonPath("$[2].month").value(tr3.getDate().format(FORMAT)))
                                .andExpect(jsonPath("$[2].total").value(decemberTotal));
        }

        @Test
        void getMonthlyExpenses_withInvalidRequest() throws Exception {
                mockMvc.perform(post("/api/analytics/monthly")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidDateRange)
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        // ---------------------
        // getSummary()
        // ---------------------
        @Test
        void getSummary_success() throws Exception {
                BigDecimal income = budget1.getLimitAmount().add(budget2.getLimitAmount());
                BigDecimal expense = tr3.getAmount().add(tr4.getAmount()).add(tr5.getAmount());
                mockMvc.perform(get("/api/analytics/summary")
                                .param("month", "2025-12")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.income").value(income))
                                .andExpect(jsonPath("$.expense").value(expense))
                                .andExpect(jsonPath("$.balance").value(income.subtract(expense)));
        }

        @Test
        void getSummary_withInvalidParam() throws Exception {
                mockMvc.perform(get("/api/analytics/summary")
                                .param("month", "-5-10000")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        // ---------------------
        // getCategoryBudgetStatus()
        // ---------------------
        @Test
        void getCategoryBudgetStatus_success() throws Exception {
                BigDecimal percentUsed = tr3.getAmount()
                                .divide(budget1.getLimitAmount(), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                mockMvc.perform(get("/api/analytics/budget/" + budget1.getId().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.category").value(budget1.getCategory().getName()))
                                .andExpect(jsonPath("$.limit").value(budget1.getLimitAmount()))
                                .andExpect(jsonPath("$.spent").value(tr3.getAmount()))
                                .andExpect(jsonPath("$.percentUsed").value(percentUsed.doubleValue()));
        }

        @Test
        void getCategoryBudgetStatus_budgetNotFound() throws Exception {
                mockMvc.perform(get("/api/analytics/budget/" + UUID.randomUUID().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // getUpcomingRecurringPayments()
        // ---------------------
        @Test
        void getUpcomingRecurringPayments_success() throws Exception {
                BigDecimal expected = rtr1.getAmount().add(rtr2.getAmount()).add(rtr3.getAmount());
                mockMvc.perform(get("/api/analytics/upcoming")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].month").value(rtr1.getNextExecutionDate().format(FORMAT)))
                                .andExpect(jsonPath("$[0].expectedAmount").value(expected))
                                .andExpect(jsonPath("$[1].month").value(rtr4.getNextExecutionDate().format(FORMAT)))
                                .andExpect(jsonPath("$[1].expectedAmount").value(rtr4.getAmount()));

        }

        private CustomUserPrincipal principal() {
                return new CustomUserPrincipal(user, Map.of(
                                "sub", "test-sub",
                                "email", "test@email.com"));
        }

}

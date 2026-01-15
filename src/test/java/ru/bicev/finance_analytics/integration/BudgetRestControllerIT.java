package ru.bicev.finance_analytics.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.util.CategoryType;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BudgetRestControllerIT {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepo;

        @Autowired
        CategoryRepository categoryRepo;

        @Autowired
        BudgetRepository budgetRepo;

        private User user;
        private Category category1;
        private Category category2;
        private Budget budget1;
        private Budget budget2;

        private String request = """
                        {
                            "categoryId": "%s",
                            "month": "2025-04",
                            "amount": "11300.00"
                        }
                        """;

        private String invalidRequest = """
                        {
                            "categoryId": "%s",
                            "month": "aa-123",
                            "amount": "-0.0001"
                        }
                        """;
        private String updateRequest = """
                        {
                            "categoryId": "%s",
                            "month": "2025-12",
                            "limitAmount": "777.00"
                        }
                        """;

        @BeforeEach
        void setUp() {
                user = userRepo.save(
                                User.builder()
                                                .provider("google")
                                                .providerId("test-sub")
                                                .email("test@email.com")
                                                .name("Test user")
                                                .avatarUrl("avatar.png")
                                                .createdAt(LocalDateTime.now())
                                                .lastLoginAt(LocalDateTime.now())
                                                .build());
                category1 = categoryRepo.save(
                                Category.builder()
                                                .name("Test")
                                                .createdAt(LocalDateTime.now())
                                                .type(CategoryType.EXPENSE)
                                                .user(user)
                                                .color("#00ff88ff")
                                                .build());
                category2 = categoryRepo.save(
                                Category.builder()
                                                .name("Another Test")
                                                .createdAt(LocalDateTime.now())
                                                .type(CategoryType.EXPENSE)
                                                .user(user)
                                                .color("#f80707ff")
                                                .build());
                budget1 = budgetRepo.save(Budget.builder()
                                .category(category1)
                                .createdAt(LocalDateTime.now())
                                .limitAmount(BigDecimal.valueOf(10000.00))
                                .user(user)
                                .month(YearMonth.of(2025, 6))
                                .build());
                budget2 = budgetRepo.save(Budget.builder()
                                .category(category2)
                                .createdAt(LocalDateTime.now())
                                .limitAmount(BigDecimal.valueOf(10000.00))
                                .user(user)
                                .month(YearMonth.of(2025, 7))
                                .build());
        }

        

        // ---------------------
        // createBudget()
        // ---------------------
        @Test
        void createBudget_success() throws Exception {
                mockMvc.perform(post("/api/budgets")
                                .with(oauth2Login().oauth2User(principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request.formatted(category1.getId().toString())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.categoryId").value(category1.getId().toString()))
                                .andExpect(jsonPath("$.categoryName").value(category1.getName()))
                                .andExpect(jsonPath("$.limitAmount").value(11300.00))
                                .andExpect(jsonPath("$.month").value("2025-04"));
        }

        @Test
        void createBudget_categoryNotFound() throws Exception {
                mockMvc.perform(post("/api/budgets")
                                .with(oauth2Login().oauth2User(principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request.formatted(UUID.randomUUID().toString())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        void createBudget_invalidRequest() throws Exception {
                mockMvc.perform(post("/api/budgets")
                                .with(oauth2Login().oauth2User(principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest.formatted(category1.getId().toString())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        // ---------------------
        // getBudgetById()
        // ---------------------
        @Test
        void getBudgetById_success() throws Exception {
                mockMvc.perform(get("/api/budgets/" + budget1.getId().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(budget1.getId().toString()))
                                .andExpect(jsonPath("$.limitAmount").value(budget1.getLimitAmount()))
                                .andExpect(jsonPath("$.month").value(budget1.getMonth().toString()));
        }

        @Test
        void getBudgetById_notFound() throws Exception {
                mockMvc.perform(get("/api/budgets/" + UUID.randomUUID().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // getAllUsersBudgets()
        // ---------------------
        @Test
        void getAllUsersBudgets_success() throws Exception {
                mockMvc.perform(get("/api/budgets")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value(budget1.getId().toString()))
                                .andExpect(jsonPath("$[1].id").value(budget2.getId().toString()))
                                .andExpect(jsonPath("$[0].limitAmount").value(budget1.getLimitAmount()))
                                .andExpect(jsonPath("$[1].limitAmount").value(budget2.getLimitAmount()))
                                .andExpect(jsonPath("$[0].month").value(budget1.getMonth().toString()))
                                .andExpect(jsonPath("$[1].month").value(budget2.getMonth().toString()));
        }

        @Test
        void getAllUsersBudgets_withYearMonth() throws Exception {
                mockMvc.perform(get("/api/budgets")
                                .with(oauth2Login().oauth2User(principal()))
                                .param("yearMonth", "2025-07"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].id").value(budget2.getId().toString()))
                                .andExpect(jsonPath("$[0].limitAmount").value(budget2.getLimitAmount()))
                                .andExpect(jsonPath("$[0].month").value(budget2.getMonth().toString()));
        }

        // ---------------------
        // updateBudget()
        // ---------------------
        @Test
        void updateBudget_success() throws Exception {
                mockMvc.perform(put("/api/budgets/" + budget2.getId().toString())
                                .with(oauth2Login().oauth2User(principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest.formatted(category1.getId().toString())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.categoryName").value(category1.getName()))
                                .andExpect(jsonPath("$.limitAmount").value(777.00))
                                .andExpect(jsonPath("$.month").value("2025-12"));
        }

        @Test
        void updateBudget_budgetNotFound() throws Exception {
                mockMvc.perform(put("/api/budgets/" + UUID.randomUUID().toString())
                                .with(oauth2Login().oauth2User(principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest.formatted(category1.getId().toString())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        void updateBudget_categoryNotFound() throws Exception {
                mockMvc.perform(put("/api/budgets/" + budget2.getId().toString())
                                .with(oauth2Login().oauth2User(principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest.formatted(UUID.randomUUID().toString())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // deleteBudget()
        // ---------------------
        @Test
        void deleteBudget_success() throws Exception {
                mockMvc.perform(delete("/api/budgets/" + budget1.getId().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteBudget_notFound() throws Exception {
                mockMvc.perform(delete("/api/budgets/" + UUID.randomUUID().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        private CustomUserPrincipal principal() {
                return new CustomUserPrincipal(user, Map.of(
                                "sub", "test-sub",
                                "email", "test@email.com"));
        }

}

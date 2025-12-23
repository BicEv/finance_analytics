package ru.bicev.finance_analytics.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.util.CategoryType;
import ru.bicev.finance_analytics.util.Frequency;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RecurringTransactionRestControllerIT {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepository;

        @Autowired
        CategoryRepository categoryRepository;

        @Autowired
        RecurringTransactionRepository recurringTransactionRepository;

        private User user;
        private Category category1;
        private RecurringTransaction tr1;
        private RecurringTransaction tr2;
        private RecurringTransaction tr3;

        private String request = """
                                {
                                        "categoryId": "%s",
                                        "amount": 20.99,
                                        "frequency": "MONTHLY",
                                        "nextExecutionDate": "2025-12-31",
                                        "description": "Test transaction",
                                        "isActive": true
                                }
                        """;
        private String invalidRequest = """
                                {
                                        "categoryId": "%s",
                                        "amount": -1.99,
                                        "frequency": "MONTHLY",
                                        "nextExecutionDate": "",
                                        "description": "Test transaction",
                                        "isActive": true
                                }
                        """;
        private String updateRequest = """
                        {
                                "amount": 9.99,
                                "frequency": "MONTHLY",
                                "nextExecutionDate": "2099-01-15",
                                "description": "Updated description"
                        }
                                        """;

        @BeforeEach
        void setUp() {
                user = userRepository.save(
                                User.builder()
                                                .provider("google")
                                                .providerId("test-sub")
                                                .email("test@email.com")
                                                .name("Test user")
                                                .avatarUrl("avatar.png")
                                                .createdAt(LocalDateTime.now())
                                                .lastLoginAt(LocalDateTime.now())
                                                .build());
                category1 = categoryRepository.save(
                                Category.builder()
                                                .name("Food")
                                                .createdAt(LocalDateTime.now())
                                                .type(CategoryType.EXPENSE)
                                                .user(user)
                                                .color("#ff0000")
                                                .build());
                tr1 = recurringTransactionRepository.save(
                                RecurringTransaction.builder()
                                                .user(user)
                                                .category(category1)
                                                .amount(BigDecimal.valueOf(9.99))
                                                .createdAt(LocalDateTime.now())
                                                .description("RecurringTr #1")
                                                .frequency(Frequency.MONTHLY)
                                                .isActive(true)
                                                .lastExecutionDate(LocalDate.of(2025, 11, 25))
                                                .nextExecutionDate(LocalDate.of(2025, 12, 25))
                                                .build());
                tr2 = recurringTransactionRepository.save(
                                RecurringTransaction.builder()
                                                .user(user)
                                                .category(category1)
                                                .amount(BigDecimal.valueOf(10.99))
                                                .createdAt(LocalDateTime.now())
                                                .description("RecurringTr #2")
                                                .frequency(Frequency.MONTHLY)
                                                .isActive(true)
                                                .lastExecutionDate(LocalDate.of(2025, 12, 01))
                                                .nextExecutionDate(LocalDate.of(2026, 01, 01))
                                                .build());
                tr3 = recurringTransactionRepository.save(
                                RecurringTransaction.builder()
                                                .user(user)
                                                .category(category1)
                                                .amount(BigDecimal.valueOf(15.99))
                                                .createdAt(LocalDateTime.now())
                                                .description("RecurringTr #3")
                                                .frequency(Frequency.YEARLY)
                                                .isActive(true)
                                                .lastExecutionDate(LocalDate.of(2025, 06, 06))
                                                .nextExecutionDate(LocalDate.of(2026, 06, 06))
                                                .build());

        }

        // ---------------------
        // createTransaction()
        // ---------------------
        @Test
        void createTransaction_success() throws Exception {
                mockMvc.perform(
                                post("/api/recurring")
                                                .with(oauth2Login().oauth2User(principal()))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request.formatted(category1.getId().toString())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.amount").value(20.99))
                                .andExpect(jsonPath("$.description").value("Test transaction"))
                                .andExpect(jsonPath("$.nextExecutionDate").value("2025-12-31"))
                                .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        void createTransaction_invalidRequest() throws Exception {
                mockMvc.perform(
                                post("/api/recurring")
                                                .with(oauth2Login().oauth2User(principal()))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(invalidRequest.formatted(category1.getId().toString())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.length()").isNotEmpty());
        }

        @Test
        void createTransaction_categoryNotFound() throws Exception {
                mockMvc.perform(
                                post("/api/recurring")
                                                .with(oauth2Login().oauth2User(principal()))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request.formatted(UUID.randomUUID().toString())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // getTransactions()
        // ---------------------
        @Test
        void getTransactions_success() throws Exception {
                mockMvc.perform(get("/api/recurring")
                                .with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].amount").value(tr1.getAmount()))
                                .andExpect(jsonPath("$[0].nextExecutionDate")
                                                .value(tr1.getNextExecutionDate().toString()))
                                .andExpect(jsonPath("$[0].description").value(tr1.getDescription()))
                                .andExpect(jsonPath("$[1].amount").value(tr2.getAmount()))
                                .andExpect(jsonPath("$[1].nextExecutionDate")
                                                .value(tr2.getNextExecutionDate().toString()))
                                .andExpect(jsonPath("$[1].description").value(tr2.getDescription()))
                                .andExpect(jsonPath("$[2].amount").value(tr3.getAmount()))
                                .andExpect(jsonPath("$[2].nextExecutionDate")
                                                .value(tr3.getNextExecutionDate().toString()))
                                .andExpect(jsonPath("$[2].description").value(tr3.getDescription()));
        }

        @Test
        void getTransactions_withParam() throws Exception {
                mockMvc.perform(get("/api/recurring")
                                .param("date", "2026-01-01")
                                .with(oauth2Login().oauth2User(principal()))).andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].amount").value(tr1.getAmount()))
                                .andExpect(jsonPath("$[0].nextExecutionDate")
                                                .value(tr1.getNextExecutionDate().toString()))
                                .andExpect(jsonPath("$[0].description").value(tr1.getDescription()))
                                .andExpect(jsonPath("$[1].amount").value(tr2.getAmount()))
                                .andExpect(jsonPath("$[1].nextExecutionDate")
                                                .value(tr2.getNextExecutionDate().toString()))
                                .andExpect(jsonPath("$[1].description").value(tr2.getDescription()));
        }

        @Test
        void getTransactions_withInvalidParam() throws Exception {
                mockMvc.perform(get("/api/recurring")
                                .param("date", "abc")
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(400));
        }

        // ---------------------
        // getTransactionById()
        // ---------------------
        @Test
        void getTransactionById_success() throws Exception {
                mockMvc.perform(get("/api/recurring/" + tr2.getId().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(tr2.getId().toString()))
                                .andExpect(jsonPath("$.amount").value(tr2.getAmount()))
                                .andExpect(jsonPath("$.frequency").value(tr2.getFrequency().name()))
                                .andExpect(jsonPath("$.nextExecutionDate")
                                                .value(tr2.getNextExecutionDate().toString()));
        }

        @Test
        void getTransactionById_notFound() throws Exception {
                mockMvc.perform(get("/api/recurring/" + UUID.randomUUID().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // updateTransaction()
        // ---------------------
        @Test
        void updateTransaction_success() throws Exception {
                mockMvc.perform(
                                put("/api/recurring/" + tr3.getId().toString())
                                                .with(oauth2Login().oauth2User(principal()))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(updateRequest))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.amount").value(9.99))
                                .andExpect(jsonPath("$.frequency").value(Frequency.MONTHLY.name()))
                                .andExpect(jsonPath("$.nextExecutionDate").value("2099-01-15"))
                                .andExpect(jsonPath("$.description").value("Updated description"));
        }

        @Test
        void updateTransaction_notFound() throws Exception {
                mockMvc.perform(
                                put("/api/recurring/" + UUID.randomUUID().toString())
                                                .with(oauth2Login().oauth2User(principal()))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(updateRequest))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // deleteTransaction()
        // ---------------------
        @Test
        void deleteTransaction_success() throws Exception {
                mockMvc.perform(delete("/api/recurring/" + tr2.getId().toString())
                                .with(oauth2Login().oauth2User(principal())))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteTransaction_notFound() throws Exception {
                mockMvc.perform(delete("/api/recurring/" + UUID.randomUUID().toString())
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

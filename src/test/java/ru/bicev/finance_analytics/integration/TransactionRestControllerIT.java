package ru.bicev.finance_analytics.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.util.CategoryType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class TransactionRestControllerIT {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepository;

        @Autowired
        CategoryRepository categoryRepository;

        @Autowired
        TransactionRepository transactionRepository;

        private User user;
        private CustomUserPrincipal principal;
        private Category category;
        private Transaction tr1;
        private Transaction tr2;
        private Transaction tr3;
        private String request = """
                        {
                            "categoryId": "%s",
                            "amount": 333.00,
                            "date": "2025-11-10",
                            "description": "Test request",
                            "isPlanned": false
                        }
                        """;
        private String invalidRequest = """
                        {
                            "categoryId": "%s",
                            "amount": -0.10,
                            "description": "Test request",
                            "isPlanned": false
                        }
                        """;
        private String updateRequest = """
                        {
                            "amount": 666.00,
                            "date": "2025-06-06",
                            "description": "Update request",
                            "isPlanned": false
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
                principal = new CustomUserPrincipal(user.getId(), Map.of());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
                                principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                category = categoryRepository.save(
                                Category.builder()
                                                .name("Food")
                                                .createdAt(LocalDateTime.now())
                                                .type(CategoryType.EXPENSE)
                                                .user(user)
                                                .color("#ff0000")
                                                .build());
                tr1 = transactionRepository.save(
                                Transaction.builder()
                                                .user(user)
                                                .category(category)
                                                .amount(BigDecimal.valueOf(900.00))
                                                .createdAt(LocalDateTime.now())
                                                .date(LocalDate.of(2025, 11, 5))
                                                .description("Transaction 1")
                                                .isPlanned(false)
                                                .build());
                tr2 = transactionRepository.save(
                                Transaction.builder()
                                                .user(user)
                                                .category(category)
                                                .amount(BigDecimal.valueOf(700.00))
                                                .createdAt(LocalDateTime.now())
                                                .date(LocalDate.of(2025, 11, 11))
                                                .description("Transaction 2")
                                                .isPlanned(false)
                                                .build());
                tr3 = transactionRepository.save(
                                Transaction.builder()
                                                .user(user)
                                                .category(category)
                                                .amount(BigDecimal.valueOf(550.50))
                                                .createdAt(LocalDateTime.now())
                                                .date(LocalDate.of(2025, 12, 7))
                                                .description("Transaction 3")
                                                .isPlanned(false)
                                                .build());
        }

        @AfterEach
        void cleanUp() {
                SecurityContextHolder.clearContext();
        }

        // ---------------------
        // createTransaction()
        // ---------------------
        @Test
        void createTransaction_success() throws Exception {
                mockMvc.perform(
                                post("/api/transactions")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request.formatted(category.getId().toString())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.amount").value(333.00))
                                .andExpect(jsonPath("$.date").value("2025-11-10"))
                                .andExpect(jsonPath("$.description").value("Test request"));
        }

        @Test
        void createTransaction_invalidRequest() throws Exception {
                mockMvc.perform(
                                post("/api/transactions")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(invalidRequest.formatted(category.getId().toString())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.length()").isNotEmpty());
        }

        @Test
        void createTransaction_categoryNotFound() throws Exception {
                mockMvc.perform(
                                post("/api/transactions")
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
                mockMvc.perform(
                                get("/api/transactions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].amount").value(tr1.getAmount()))
                                .andExpect(jsonPath("$[0].date").value(tr1.getDate().toString()))
                                .andExpect(jsonPath("$[0].description").value(tr1.getDescription()))
                                .andExpect(jsonPath("$[1].amount").value(tr2.getAmount()))
                                .andExpect(jsonPath("$[1].date").value(tr2.getDate().toString()))
                                .andExpect(jsonPath("$[1].description").value(tr2.getDescription()))
                                .andExpect(jsonPath("$[2].amount").value(tr3.getAmount()))
                                .andExpect(jsonPath("$[2].date").value(tr3.getDate().toString()))
                                .andExpect(jsonPath("$[2].description").value(tr3.getDescription()));
        }

        @Test
        void getTransactions_withParams() throws Exception {
                mockMvc.perform(
                                get("/api/transactions")
                                                .param("start", "2025-11-01")
                                                .param("end", "2025-11-30"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].amount").value(tr1.getAmount()))
                                .andExpect(jsonPath("$[0].date").value(tr1.getDate().toString()))
                                .andExpect(jsonPath("$[0].description").value(tr1.getDescription()))
                                .andExpect(jsonPath("$[1].amount").value(tr2.getAmount()))
                                .andExpect(jsonPath("$[1].date").value(tr2.getDate().toString()))
                                .andExpect(jsonPath("$[1].description").value(tr2.getDescription()));
        }

        @Test
        void getTransactions_withPartParams() throws Exception {
                mockMvc.perform(
                                get("/api/transactions")
                                                .param("start", "2025-11-01"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3));
        }

        // ---------------------
        // getTransactionById()
        // ---------------------
        @Test
        void getTransactionById_success() throws Exception {

                mockMvc.perform(
                                get("/api/transactions/" + tr3.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.amount").value(tr3.getAmount()))
                                .andExpect(jsonPath("$.date").value(tr3.getDate().toString()))
                                .andExpect(jsonPath("$.description").value(tr3.getDescription()));
        }

        @Test
        void getTransactionById_notFound() throws Exception {
                mockMvc.perform(
                                get("/api/transactions/" + UUID.randomUUID().toString()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

        // ---------------------
        // updateTransaction()
        // ---------------------
        @Test
        void updateTransaction_success() throws Exception {
                mockMvc.perform(
                                put("/api/transactions/" + tr3.getId().toString())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(updateRequest))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.amount").value(666.00))
                                .andExpect(jsonPath("$.date").value("2025-06-06"))
                                .andExpect(jsonPath("$.description").value("Update request"));
        }

        @Test
        void updateTransaction_notFound() throws Exception {
                mockMvc.perform(
                                put("/api/transactions/" + UUID.randomUUID().toString())
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
                mockMvc.perform(
                                delete("/api/transactions/" + tr1.getId().toString()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteTransaction_notFound() throws Exception {
                mockMvc.perform(
                                delete("/api/transactions/" + UUID.randomUUID().toString()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404));
        }

}

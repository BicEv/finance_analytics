package ru.bicev.finance_analytics.integration;

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

import ru.bicev.finance_analytics.entity.BudgetTemplate;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.BudgetTemplateRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.util.CategoryType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BudgetTemplateRestControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    BudgetTemplateRepository budgetTemplateRepository;

    private User user;
    private Category category;
    private Category category1;
    private Category category2;
    private BudgetTemplate template1;
    private BudgetTemplate template2;

    private String request = """
                    {
                        "categoryId": "%s",
                        "amount": "7500",
                        "active": "true",
                        "startMonth": "2025-07"
                    }
            """;

    private String invalidRequest = """
                    {
                        "categoryId": "%s",
                        "amount": "-700",
                        "active": "",
                        "startMonth": "5-7a9"
                    }
            """;

    private String updateRequest = """
                    {
                        "amount": "777",
                        "active": "false"
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
        category = categoryRepository.save(
                Category.builder()
                        .name("Test")
                        .createdAt(LocalDateTime.now())
                        .type(CategoryType.EXPENSE)
                        .user(user)
                        .color("#00ff88ff")
                        .build());
        category1 = categoryRepository.save(
                Category.builder()
                        .name("Food")
                        .createdAt(LocalDateTime.now())
                        .type(CategoryType.EXPENSE)
                        .user(user)
                        .color("#ff0000")
                        .build());
        template1 = budgetTemplateRepository.save(
                BudgetTemplate.builder()
                        .category(category1)
                        .amount(BigDecimal.valueOf(5000.00))
                        .startMonth(YearMonth.of(2025, 1))
                        .user(user)
                        .active(true)
                        .build());
        category2 = categoryRepository.save(
                Category.builder()
                        .name("Entertainment")
                        .createdAt(LocalDateTime.now())
                        .type(CategoryType.EXPENSE)
                        .user(user)
                        .color("#c300ffff")
                        .build());
        template2 = budgetTemplateRepository.save(
                BudgetTemplate.builder()
                        .category(category2)
                        .amount(BigDecimal.valueOf(2500.00))
                        .startMonth(YearMonth.of(2025, 6))
                        .user(user)
                        .active(true)
                        .build());
    }

    // ---------------------
    // createBudgetTemplate()
    // ---------------------
    @Test
    void createBudgetTemplate_success() throws Exception {
        mockMvc.perform(post("/api/templates")
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.formatted(category.getId().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(BigDecimal.valueOf(7500)))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.startMonth").value("07.2025"));
    }

    @Test
    void createBudgetTemplate_categoryNotFound() throws Exception {
        mockMvc.perform(post("/api/templates")
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.formatted(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

    }

    @Test
    void createBudgetTemplate_invalidRequest() throws Exception {
        mockMvc.perform(post("/api/templates")
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest.formatted(category.getId().toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBudgetTemplate_templateExists() throws Exception {
        mockMvc.perform(post("/api/templates")
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.formatted(category1.getId().toString())))
                .andExpect(status().isBadRequest());
    }

    // ---------------------
    // getTemplateById()
    // ---------------------
    @Test
    void getTemplateById_success() throws Exception {
        mockMvc.perform(get("/api/templates/" + template1.getId().toString())
                .with(oauth2Login().oauth2User(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(template1.getId().toString()))
                .andExpect(jsonPath("$.amount").value(template1.getAmount()))
                .andExpect(jsonPath("$.active").value(template1.isActive()));
    }

    @Test
    void getTemplateById_templateNotFound() throws Exception {
        mockMvc.perform(get("/api/templates/" + UUID.randomUUID().toString())
                .with(oauth2Login().oauth2User(principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ---------------------
    // getAllTemplates()
    // ---------------------
    @Test
    void getAllTemplates_success() throws Exception {
        mockMvc.perform(get("/api/templates")
                .with(oauth2Login().oauth2User(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(template1.getAmount()))
                .andExpect(jsonPath("$[0].active").value(template1.isActive()))
                .andExpect(jsonPath("$[1].amount").value(template2.getAmount()))
                .andExpect(jsonPath("$[1].active").value(template2.isActive()));
    }

    // ---------------------
    // updateTemplate()
    // ---------------------
    @Test
    void updateTemplate_success() throws Exception {
        mockMvc.perform(put("/api/templates/" + template1.getId().toString())
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(BigDecimal.valueOf(777)))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateTemplate_notFound() throws Exception {
        mockMvc.perform(put("/api/templates/" + UUID.randomUUID().toString())
                .with(oauth2Login().oauth2User(principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ---------------------
    // deleteTemplate()
    // ---------------------
    @Test
    void deleteTemplate_success() throws Exception {
        mockMvc.perform(delete("/api/templates/" + template2.getId().toString())
                .with(oauth2Login().oauth2User(principal())))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTemplate_notFound() throws Exception {
        mockMvc.perform(delete("/api/templates/" + UUID.randomUUID().toString())
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

package ru.bicev.finance_analytics.integration;

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
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.util.CategoryType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class CategoryRestControllerIT {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        UserRepository userRepository;

        @Autowired
        CategoryRepository categoryRepository;

        private User user;
        private CustomUserPrincipal principal;
        private Category cat1;
        private Category cat2;
        private String request = """
                                                {
                                                  "name": "Test category",
                                                  "type": "EXPENSE",
                                                  "color": "#ff0000"
                                                }
                        """;
        private String invalidRequest = """
                                                {
                                                  "name": "",
                                                  "type": "",
                                                  "color": ""
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
                cat1 = categoryRepository.save(Category.builder()
                                .name("Food")
                                .createdAt(LocalDateTime.now())
                                .type(CategoryType.EXPENSE)
                                .user(user)
                                .color("#ff0000")
                                .build());

                cat2 = categoryRepository.save(Category.builder()
                                .name("Salary")
                                .createdAt(LocalDateTime.now())
                                .type(CategoryType.INCOME)
                                .user(user)
                                .color("#000000ff")
                                .build());
        }

        @AfterEach
        void cleanUp() {
                SecurityContextHolder.clearContext();
        }

        // ---------------------
        // createCategory()
        // ---------------------
        @Test
        void createCategory_success() throws Exception {
                mockMvc.perform(
                                post("/api/categories")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.name").value("Test category"))
                                .andExpect(jsonPath("$.type").value("EXPENSE"));
        }

        @Test
        void createCategory_invalidData() throws Exception {
                mockMvc.perform(
                                post("/api/categories")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(invalidRequest))
                                .andExpect(status().isBadRequest());

        }

        // ---------------------
        // getCategories()
        // ---------------------
        @Test
        void getCategories_success() throws Exception {
                mockMvc.perform(get("/api/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].name").value("Food"))
                                .andExpect(jsonPath("$[1].name").value("Salary"));
        }

        @Test
        void getCategories_successWithParam() throws Exception {
                mockMvc.perform(get("/api/categories")
                                .param("type", "EXPENSE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].name").value("Food"));

        }

        // ---------------------
        // getCategoryById()
        // ---------------------
        @Test
        void getCategoryById_success() throws Exception {
                mockMvc.perform(get("/api/categories/" + cat1.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Food"))
                                .andExpect(jsonPath("$.type").value("EXPENSE"));
        }

        @Test
        void getCategoryById_notFound() throws Exception {
                mockMvc.perform(get("/api/categories/" + UUID.randomUUID().toString()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("404"));
        }

        // ---------------------
        // updateCategory()
        // ---------------------
        @Test
        void updateCategory_success() throws Exception {
                mockMvc.perform(put("/api/categories/" + cat1.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Test category"))
                                .andExpect(jsonPath("$.type").value("EXPENSE"));
        }

        @Test
        void updateCategory_notFound() throws Exception {
                mockMvc.perform(put("/api/categories/" + UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("404"));

        }

        @Test
        void updateCategory_invalidData() throws Exception {
                mockMvc.perform(put("/api/categories/" + cat2.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                                .andExpect(status().isBadRequest());
        }

        // ---------------------
        // deleteCategory()
        // ---------------------
        @Test
        void deleteCategory_success() throws Exception {
                mockMvc.perform(delete("/api/categories/" + cat1.getId().toString()))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteCategory_notFound() throws Exception {
                mockMvc.perform(delete("/api/categories/" + UUID.randomUUID().toString()))
                                .andExpect(status().isNotFound());
        }

}

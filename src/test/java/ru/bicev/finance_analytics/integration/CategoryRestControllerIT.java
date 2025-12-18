package ru.bicev.finance_analytics.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CategoryRestControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    private User user;

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
    }

    @Test
    void createCategory_success() throws Exception {
        mockMvc.perform(
                post("/api/categories")
                        .with(oauth2Login().oauth2User(principal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                      "name": "Test category",
                                      "type": "EXPENSE",
                                      "color": "#ff0000"
                                    }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test category"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(user, Map.of(
                "sub", "test-sub",
                "email", "test@email.com"));
    }

    

}

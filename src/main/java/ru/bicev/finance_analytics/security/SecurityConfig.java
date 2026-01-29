package ru.bicev.finance_analytics.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import ru.bicev.finance_analytics.security.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final OAuth2AuthenticationSuccessHandler successHandler;

        @Value("${app.frontend.redirect-url}")
        private String REDIRECT_URL;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        OAuth2AuthenticationSuccessHandler successHandler) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.successHandler = successHandler;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(REDIRECT_URL));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));

                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", config);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/actuator/**",
                                                                "/error",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(successHandler))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .logoutSuccessHandler((req, res, auth) -> {
                                                        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", "")
                                                                        .path("/")
                                                                        .maxAge(0)
                                                                        .httpOnly(true)
                                                                        .secure(true)
                                                                        .sameSite("Lax")
                                                                        .build();
                                                        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                                                        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                                                }));
                return http.build();
        }

}

package ru.bicev.finance_analytics.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import ru.bicev.finance_analytics.security.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final OAuth2AuthenticationSuccessHandler successHandler;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        OAuth2AuthenticationSuccessHandler successHandler) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.successHandler = successHandler;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .cors(Customizer.withDefaults())
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
                                                .successHandler(successHandler)
                                                .defaultSuccessUrl("http://localhost:5173", true))
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

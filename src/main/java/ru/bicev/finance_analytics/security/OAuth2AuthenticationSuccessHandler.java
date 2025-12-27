package ru.bicev.finance_analytics.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.bicev.finance_analytics.security.jwt.JwtService;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.security.secure-cookie}")
    private boolean isProd;

    @Value("${app.frontend.redirect-url")
    private String redirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        String jwt = jwtService.generateToken(principal.getUser().getId());

        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", jwt)
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect(redirectUrl);
    }

}

package ru.bicev.finance_analytics.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.security.jwt.JwtService;
import ru.bicev.finance_analytics.service.UserService;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;

    @Value("${app.security.secure-cookie}")
    private boolean isProd;

    @Value("${app.frontend.redirect-url")
    private String redirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Object principalObj = authentication.getPrincipal();

        CustomUserPrincipal principal;
        if (principalObj instanceof CustomUserPrincipal) {
            principal = (CustomUserPrincipal) principalObj;
        } else if (principalObj instanceof OAuth2User oAuth2User) {
            User user = userService.getOrCreateOAuthUser(
                    ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId(),
                    oAuth2User);
            principal = new CustomUserPrincipal(user, oAuth2User.getAttributes());
        } else {
            throw new IllegalStateException("Unsupported principal type: " + principalObj.getClass());
        }

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

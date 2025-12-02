package ru.bicev.finance_analytics.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.service.UserService;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public OAuth2AuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String requestUri = request.getRequestURI();
        String provider = extractProvider(requestUri);

        User localUser = userService.getOrCreateOAuthUser(provider, oAuth2User);

        CustomUserPrincipal principal = new CustomUserPrincipal(localUser);

        Authentication auth = new CustomUserAuthentication(principal);

        SecurityContextHolder.getContext().setAuthentication(auth);

        response.sendRedirect("/");

    }

    private String extractProvider(String uri) {
        if (uri.contains("google"))
            return "google";
        if (uri.contains("github"))
            return "github";
        return "unknown";
    }

}

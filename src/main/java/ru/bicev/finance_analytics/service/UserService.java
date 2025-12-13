package ru.bicev.finance_analytics.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;

@Service
public class UserService {

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getOrCreateOAuthUser(String provider, OAuth2User oAuth2User) {
        String providerId = extractProviderId(provider, oAuth2User);

        logger.debug("getOrCreateOAuthUser(), provider: {}; providerId: {}", provider, providerId);
        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);

        if (existing.isPresent()) {
            User u = existing.get();
            u.setLastLoginAt(LocalDateTime.now());
            logger.debug("User is found in db : {}", u.getId());
            return userRepository.save(u);
        }

        String email = extractEmail(provider, oAuth2User);
        String name = extractName(provider, oAuth2User);
        String avatar = extractAvatar(provider, oAuth2User);

        User newUser = User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .name(name)
                .avatarUrl(avatar)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
        logger.debug("User created");
        return userRepository.save(newUser);
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal principal))
            throw new IllegalStateException("Not authenticated");

        return principal.getUser();
    }

    private String extractProviderId(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("sub");
            case "github" -> String.valueOf(u.getAttribute("id"));
            default -> u.getName();
        };
    }

    private String extractEmail(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("email");
            default -> null;
        };
    }

    private String extractName(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("name");
            case "github" -> u.getAttribute("login");
            default -> "Unknown";
        };
    }

    private String extractAvatar(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("picture");
            case "github" -> u.getAttribute("avatar_url");
            default -> null;
        };
    }

}

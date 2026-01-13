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

/** Сервис для управления пользователями */
@Service
public class UserService {

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Создает или получает пользователя соответствующего провайдеру и OAuth2User
     * 
     * @param provider   провайдер данных аутенфикации
     * @param oAuth2User пользователь аутентификации
     * @return данные, соответсвеющие пользоваетлю в системе
     */
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

    /**
     * Служебный метод, который возвращает текущего пользователя
     * 
     * @return текущий пользователь в системе
     * @throws IllegalStateException если текущий пользователь не аутентинфицирован
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal principal))
            throw new IllegalStateException("Not authenticated");

        return principal.getUser();
    }

    /**
     * Служебный метод извлекающий идентификатор провайдера
     * 
     * @param provider данные провайдера аутентинфикации
     * @param u        пользователь аутентинфикации
     * @return строка с идентификатором провайдера
     */
    private String extractProviderId(String provider, OAuth2User u) {
        Object raw = switch (provider) {
            case "google" -> u.getAttributes().get("sub");
            case "github" -> u.getAttributes().get("id");
            default -> u.getName();
        };
        return String.valueOf(raw);
    }

    /**
     * Служебный метод для извлечения email из данных аутентинфикации
     * 
     * @param provider данные провайдера аутентинфикации
     * @param u        пользователь аутентинфикации
     * @return строка с email пользователя
     */
    private String extractEmail(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("email");
            default -> null;
        };
    }

    /**
     * Служебный метод для извлечения имени пользователя из данных аутентинфикации
     * 
     * @param provider данные провайдера аутентинфикации
     * @param u        пользователь аутентинфикации
     * @return строка с именем пользователя
     */
    private String extractName(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("name");
            case "github" -> u.getAttribute("login");
            default -> "Unknown";
        };
    }

    /**
     * Служебный метод для извлечения аватара пользователя из данных аутентинфикации
     * 
     * @param provider данные провайдера аутентинфикации
     * @param u        пользователь аутентинфикации
     * @return строка с адресом аватара пользователя
     */
    private String extractAvatar(String provider, OAuth2User u) {
        return switch (provider) {
            case "google" -> u.getAttribute("picture");
            case "github" -> u.getAttribute("avatar_url");
            default -> null;
        };
    }

}

package ru.bicev.finance_analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.security.CustomUserPrincipal;
import ru.bicev.finance_analytics.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private UserService userService;

    private String provider;
    private String providerId;
    private String email;
    private String name;
    private String avatar;

    @BeforeEach
    public void setUp() {
        provider = "google";
        providerId = "sub";
        email = "test@email.com";
        name = "John Doe";
        avatar = "http://avatar.com/img.png";
    }

    @AfterEach
    public void clear() {
        SecurityContextHolder.clearContext();
    }

    // --------------------------------------------------------
    // getOrCreateOAuthUser()
    // --------------------------------------------------------
    @Test
    void testGetOrCreateOAuthUser_Success() {
        when(userRepository.findByProviderAndProviderId(provider, providerId)).thenReturn(Optional.empty());

        when(oAuth2User.getAttribute("sub")).thenReturn(providerId);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        when(oAuth2User.getAttribute("picture")).thenReturn(avatar);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        userService.getOrCreateOAuthUser(provider, oAuth2User);

        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals(provider, saved.getProvider());
        assertEquals(providerId, saved.getProviderId());
        assertEquals(email, saved.getEmail());
        assertEquals(name, saved.getName());
        assertEquals(avatar, saved.getAvatarUrl());

    }

    @Test
    void testGetOrCreateOAuthUser_UserExists() {
        User existing = User.builder()
                .id(1L)
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .email(email)
                .avatarUrl(avatar)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();

        LocalDateTime old = existing.getLastLoginAt();

        when(oAuth2User.getAttribute("sub")).thenReturn(providerId);
        when(userRepository.findByProviderAndProviderId(provider, providerId)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.getOrCreateOAuthUser(provider, oAuth2User);

        assertNotNull(result);
        assertEquals(existing.getId(), result.getId());
        assertEquals(existing.getEmail(), result.getEmail());
        assertTrue(result.getLastLoginAt().isAfter(old));

        verify(userRepository, times(1)).findByProviderAndProviderId(provider, providerId);
        verify(userRepository, times(1)).save(existing);
    }

    // --------------------------------------------------------
    // getCurrentUser()
    // --------------------------------------------------------
    @Test
    void testGetCurrentUser_success() {
        User user = User.builder().id(10L).email(email).build();
        CustomUserPrincipal principal = new CustomUserPrincipal(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);

        User result = userService.getCurrentUser();

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void testGetCurrentUser_noAuth() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        assertThrows(IllegalStateException.class, () -> userService.getCurrentUser());
    }

    @Test
    void testGetCurrentUser_invalidPrincipal() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("strange-principal");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);

        assertThrows(IllegalStateException.class, () -> userService.getCurrentUser());

    }

}

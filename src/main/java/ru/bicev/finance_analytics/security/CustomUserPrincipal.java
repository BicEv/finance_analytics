package ru.bicev.finance_analytics.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;


public class CustomUserPrincipal implements OAuth2User {

    private final Long userId;
    private final Map<String, Object> attributes;

    public CustomUserPrincipal(Long userId, Map<String, Object> attributes) {
        this.userId = userId;
        this.attributes = attributes;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public <A> A getAttribute(String name) {
        return (A) attributes.get(name);
    }

}

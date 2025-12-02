package ru.bicev.finance_analytics.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class CustomUserAuthentication extends AbstractAuthenticationToken {

    private final CustomUserPrincipal principal;

    public CustomUserAuthentication(CustomUserPrincipal principal) {
        super(principal.getAuthorities());
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return principal;
    }

}

package com.example.testtracker.auth;

import com.example.testtracker.AccountLogin.AccountLogin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AccountLoginPrincipal implements UserDetails {
    private final UUID loginId;
    private final UUID accountId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;

    public AccountLoginPrincipal(AccountLogin accountLogin) {
        this.loginId = accountLogin.getId();
        this.accountId = accountLogin.getAccountId();
        this.email = accountLogin.getEmail();
        this.passwordHash = accountLogin.getPasswordHash();
        this.enabled = accountLogin.isEnabled();
    }

    public UUID getLoginId() {
        return loginId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}

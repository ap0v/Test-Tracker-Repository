package com.example.testtracker.auth;

import com.example.testtracker.AccountLogin.AccountLogin;
import com.example.testtracker.AccountLogin.AccountLoginRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountLoginRepository accountLoginRepository;

    public AccountUserDetailsService(AccountLoginRepository accountLoginRepository) {
        this.accountLoginRepository = accountLoginRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AccountLogin accountLogin = accountLoginRepository.findByNormalizedEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
                
        return new AccountLoginPrincipal(accountLogin);
    }
    
}

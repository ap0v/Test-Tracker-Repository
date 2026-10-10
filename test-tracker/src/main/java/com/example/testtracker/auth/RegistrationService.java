package com.example.testtracker.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.testtracker.Account.Account;
import com.example.testtracker.Account.AccountRepository;
import com.example.testtracker.AccountLogin.AccountLogin;
import com.example.testtracker.AccountLogin.AccountLoginRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class RegistrationService {
    private final AccountRepository accountRepository;
    private final AccountLoginRepository accountLoginRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            AccountRepository accountRepository,
            AccountLoginRepository accountLoginRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.accountLoginRepository = accountLoginRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerAccount(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accountLoginRepository.findByNormalizedEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        if (request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }

        String hash = passwordEncoder.encode(request.password());

        Account account = accountRepository.save(
            new Account(request.accountName().trim())
        );

        accountLoginRepository.save(
            new AccountLogin(account.getId(), email, hash)
        );
    }
}

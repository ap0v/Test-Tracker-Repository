package com.example.testtracker.AccountLogin;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for managing AccountLogin entities.
 */
public interface AccountLoginRepository extends JpaRepository<AccountLogin, UUID> {
    @Query(value = """
        select * from account_logins where lower(btrim(email)) = lower(btrim(:email))
    """, nativeQuery = true)
    Optional<AccountLogin> findByNormalizedEmail(@Param("email") String email);
}

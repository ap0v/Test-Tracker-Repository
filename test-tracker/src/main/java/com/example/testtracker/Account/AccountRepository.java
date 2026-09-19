package com.example.testtracker.Account;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing Account entities.
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {
}

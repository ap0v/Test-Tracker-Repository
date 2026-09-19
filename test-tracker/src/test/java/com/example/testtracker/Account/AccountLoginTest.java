package com.example.testtracker.Account;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class AccountLoginTest {

    @Test
    void serializationExcludesPasswordHash() {
        String passwordHash = "encoded-password-for-serialization-test";
        AccountLogin login = new AccountLogin(UUID.randomUUID(), "tester@example.com", passwordHash);

        String json = JsonMapper.builder().build().writeValueAsString(login);

        assertTrue(json.contains("tester@example.com"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains(passwordHash));
    }
}

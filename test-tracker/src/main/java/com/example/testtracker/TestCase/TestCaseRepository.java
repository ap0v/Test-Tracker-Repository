package com.example.testtracker.TestCase;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 
 * TestCaseRepository
 */
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {
}

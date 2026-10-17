package com.navas.gastosapi.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByUserIdAndSpentOnBetween(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);
}

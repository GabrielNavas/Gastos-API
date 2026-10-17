package com.navas.gastosapi.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(Long id, BigDecimal amount, String description, LocalDate spentOn, Long categoryId) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getAmount(), expense.getDescription(),
                expense.getSpentOn(), expense.getCategoryId());
    }
}

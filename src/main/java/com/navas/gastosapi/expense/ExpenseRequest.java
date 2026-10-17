package com.navas.gastosapi.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull(message = "valor é obrigatório")
        @DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "valor inválido (até 10 inteiros e 2 decimais)")
        BigDecimal amount,

        @Size(max = 255, message = "descrição deve ter no máximo 255 caracteres")
        String description,

        @NotNull(message = "data é obrigatória")
        LocalDate spentOn,

        Long categoryId) {
}

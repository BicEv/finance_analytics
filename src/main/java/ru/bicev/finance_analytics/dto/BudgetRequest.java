package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BudgetRequest(@NotNull(message = "Category id cannot be null") UUID categoryId,
        @NotNull(message = "Month cannot be null") YearMonth month,
        @NotNull(message = "Amount cannot be null") @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be bigger than zero") BigDecimal amount) {

}

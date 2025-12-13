package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(@NotNull(message = "Category id cannot be null") UUID categoryId,
                @NotNull(message = "Amount cannot be null") @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be bigger than zero") BigDecimal amount,
                @NotNull(message = "Date cannot be null") LocalDate date,
                String description,
                boolean isPlanned) {

}

package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ru.bicev.finance_analytics.util.Frequency;

public record RecurringTransactionRequest(
                @NotNull(message = "Category id cannot be null") UUID categoryId,
                @NotNull(message = "Amount cannot be null") @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be bigger than zero") BigDecimal amount,
                @NotNull(message = "Frequency cannot be null") Frequency frequency,
                @NotNull(message = "Next execution day cannot be null") LocalDate nextExecutionDate, 
                String description,
                Boolean isActive) {

}

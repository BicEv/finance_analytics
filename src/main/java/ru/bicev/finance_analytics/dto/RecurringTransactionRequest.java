package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import ru.bicev.finance_analytics.util.Frequency;

public record RecurringTransactionRequest(UUID categoryId, BigDecimal amount, Frequency frequency,
        LocalDate nextExecutionDate, String description, boolean isActive) {

}

package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringTransactionDto(UUID id, UUID categoryId, String categoryName, BigDecimal amount,
        String frequency, String description, LocalDate nextExecutionDate, boolean isActive) {

}

package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionDto(UUID id, UUID categoryId, String categoryName, BigDecimal amount, LocalDate date,
        String description, boolean isPlanned) {

}

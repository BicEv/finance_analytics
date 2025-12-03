package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(UUID accountId, UUID categoryId, BigDecimal amount, LocalDate date, String description,
        boolean isPlanned) {

}

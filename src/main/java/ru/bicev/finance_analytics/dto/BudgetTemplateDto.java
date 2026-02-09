package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record BudgetTemplateDto(UUID id, UUID categoryId, String categoryName, BigDecimal amount, boolean active,
        YearMonth startMonth) {
}

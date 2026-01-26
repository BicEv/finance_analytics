package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetTemplateDto(UUID id, UUID categoryId, String categoryName, BigDecimal amount, boolean active,
        String startMonth) {
}

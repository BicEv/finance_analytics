package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetTemplateDto(UUID id, BigDecimal amount, boolean active, String startMonth) {
}

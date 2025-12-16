package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record BudgetTemplateUpdateRequest(UUID categoryId, BigDecimal amount, Boolean active, YearMonth startMonth) {

}

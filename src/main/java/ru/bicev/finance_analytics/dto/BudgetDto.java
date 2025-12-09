package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record BudgetDto(UUID id, UUID categoryId, String categoryName, YearMonth month, BigDecimal limitAmount) {

}

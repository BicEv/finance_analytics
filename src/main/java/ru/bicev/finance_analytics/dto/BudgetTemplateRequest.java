package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BudgetTemplateRequest(
        @NotNull UUID categoryId,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
        Boolean active,
        @NotNull YearMonth startMonth) {

}

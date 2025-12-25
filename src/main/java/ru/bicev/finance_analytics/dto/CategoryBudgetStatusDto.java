package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;


public record CategoryBudgetStatusDto(
        String category,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal percentUsed) {

}

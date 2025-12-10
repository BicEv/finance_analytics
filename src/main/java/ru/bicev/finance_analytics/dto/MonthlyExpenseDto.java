package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;

public record MonthlyExpenseDto(String month, BigDecimal total) {

}

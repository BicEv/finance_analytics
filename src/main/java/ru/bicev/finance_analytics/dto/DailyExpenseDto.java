package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyExpenseDto(LocalDate date, BigDecimal amount) {

}

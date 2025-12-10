package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;

public record RecurringForecastDto(String month, BigDecimal expectedAmount) {

}

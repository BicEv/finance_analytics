package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;

public record RecurringForecastDto(String month, BigDecimal expectedAmount) {

    @ConstructorProperties({ "month", "expectedAmount" })
    public RecurringForecastDto(String month, BigDecimal expectedAmount) {
        this.month = month;
        this.expectedAmount = expectedAmount;
    }

}

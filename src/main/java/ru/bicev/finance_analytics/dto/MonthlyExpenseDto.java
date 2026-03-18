package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;

public record MonthlyExpenseDto(String month, BigDecimal total) {

    @ConstructorProperties({ "month", "total" })
    public MonthlyExpenseDto(String month, BigDecimal total) {
        this.month = month;
        this.total = total;
    }

}

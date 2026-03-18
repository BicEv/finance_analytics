package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyExpenseDto(LocalDate date, BigDecimal amount) {

    @ConstructorProperties({ "date", "amount" })
    public DailyExpenseDto(LocalDate date, BigDecimal amount) {
        this.date = date;
        this.amount = amount;
    }

}

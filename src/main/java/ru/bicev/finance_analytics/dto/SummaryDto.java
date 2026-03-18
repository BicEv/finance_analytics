package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;

public record SummaryDto(BigDecimal income, BigDecimal expense, BigDecimal balance) {

    @ConstructorProperties({ "income", "expense", "balance" })
    public SummaryDto(BigDecimal income, BigDecimal expense, BigDecimal balance) {
        this.income = income;
        this.expense = expense;
        this.balance = balance;
    }

}

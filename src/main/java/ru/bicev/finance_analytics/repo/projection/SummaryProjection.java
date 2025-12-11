package ru.bicev.finance_analytics.repo.projection;

import java.math.BigDecimal;

public interface SummaryProjection {
    BigDecimal getIncome();

    BigDecimal getExpense();

}

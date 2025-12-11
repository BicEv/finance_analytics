package ru.bicev.finance_analytics.repo.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MonthlyExpenseProjection {
    LocalDateTime getMonth();

    BigDecimal getTotalAmount();

}

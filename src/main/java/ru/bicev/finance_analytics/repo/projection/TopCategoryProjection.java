package ru.bicev.finance_analytics.repo.projection;

import java.math.BigDecimal;

public interface TopCategoryProjection {
    String getCategoryName();
    BigDecimal getTotalAmount();

}

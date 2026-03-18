package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;

public record CategoryExpenseDto(String category, BigDecimal total) {

    @ConstructorProperties({ "category", "total" })
    public CategoryExpenseDto(String category, BigDecimal total) {
        this.category = category;
        this.total = total;
    }

}

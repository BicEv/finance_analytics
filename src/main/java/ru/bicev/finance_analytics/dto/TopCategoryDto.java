package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;

public record TopCategoryDto(String category, BigDecimal total) {

    @ConstructorProperties({ "category", "total" })
    public TopCategoryDto(String category, BigDecimal total) {
        this.category = category;
        this.total = total;
    }

}

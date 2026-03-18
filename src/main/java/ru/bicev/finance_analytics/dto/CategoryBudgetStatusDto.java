package ru.bicev.finance_analytics.dto;

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record CategoryBudgetStatusDto(
                String category,
                BigDecimal limit,
                BigDecimal spent,
                BigDecimal percentUsed) {

        @ConstructorProperties({ "category", "limit", "spent", "percentUsed" })
        public CategoryBudgetStatusDto(String category,
                        BigDecimal limit,
                        BigDecimal spent,
                        BigDecimal percentUsed) {
                this.category = category;
                this.limit = limit;
                this.spent = spent;
                this.percentUsed = percentUsed;
        }

        public CategoryBudgetStatusDto normilize() {
                return new CategoryBudgetStatusDto(category,
                                limit != null ? limit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                                spent != null ? spent.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                                percentUsed != null ? percentUsed.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }

}

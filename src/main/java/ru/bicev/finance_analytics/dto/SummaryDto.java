package ru.bicev.finance_analytics.dto;

import java.math.BigDecimal;

public record SummaryDto(BigDecimal income, BigDecimal expense, BigDecimal balance) {

}

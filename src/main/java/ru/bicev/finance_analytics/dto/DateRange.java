package ru.bicev.finance_analytics.dto;

import java.time.LocalDate;
import java.time.YearMonth;

public record DateRange(LocalDate start, LocalDate end) {

    public static DateRange ofMonth(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }

    public static DateRange lastMonths(int months) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(months).withDayOfMonth(1);
        return new DateRange(start, end);
    }

    public boolean contains(LocalDate date) {
        return (date.isEqual(start) || date.isAfter(start)) && (date.isBefore(end) || date.isEqual(end));
    }

}

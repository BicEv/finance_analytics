package ru.bicev.finance_analytics.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;

public class TestUtil {

    private static final LocalDateTime NOW = LocalDateTime.now();

    public static List<Transaction> generateTransactions(User user, Category category, int number, int year,
            int month) {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i <= number; i++) {
            transactions.add(
                    Transaction.builder()
                            .user(user)
                            .category(category)
                            .amount(BigDecimal.valueOf(i * 100))
                            .date(LocalDate.of(year, month, i))
                            .createdAt(NOW)
                            .description("Transaction " + i)
                            .isPlanned(false)
                            .build());
        }
        return transactions;

    }

    public static List<RecurringTransaction> generateRecurringTransactions(User user, Category category, int number,
            int year, int month) {
        List<RecurringTransaction> transactions = new ArrayList<>();
        for (int i = 1; i <= number; i++) {
            LocalDate execution = LocalDate.now();
            transactions.add(
                    RecurringTransaction.builder()
                            .user(user)
                            .category(category)
                            .amount(BigDecimal.valueOf(i * 10))
                            .createdAt(NOW)
                            .description("Recurring transaction " + i)
                            .frequency(Frequency.MONTHLY)
                            .isActive(true)
                            .lastExecutionDate(execution.minusDays(i))
                            .nextExecutionDate(execution.plusDays(30 - i))
                            .build());
        }
        return transactions;
    }

    public static Transaction generateTransaction(User user, Category category, BigDecimal amount, int year, int month,
            int day) {
        return Transaction.builder()
                .user(user)
                .category(category)
                .amount(amount)
                .date(LocalDate.of(year, month, day))
                .createdAt(NOW)
                .isPlanned(false)
                .description("Transaction")
                .build();
    }

    public static RecurringTransaction generateRecurringTransaction(User user, Category category, BigDecimal amount,
            int year, int month, int day) {
        LocalDate execution = LocalDate.of(year, month, day);
        return RecurringTransaction.builder()
                .user(user)
                .category(category)
                .amount(amount)
                .isActive(true)
                .createdAt(NOW)
                .description("Recurring transaction")
                .lastExecutionDate(execution.minusMonths(1))
                .nextExecutionDate(execution)
                .frequency(Frequency.MONTHLY)
                .build();
    }

    public static Category generateCategory(User user, CategoryType type, String name) {
        return Category.builder()
                .user(user)
                .type(type)
                .createdAt(NOW)
                .name(name)
                .build();

    }

    public static Budget generateBudget(User user, Category category, BigDecimal amount, YearMonth month) {
        return Budget.builder()
                .user(user)
                .category(category)
                .amount(amount)
                .createdAt(NOW)
                .month(month)
                .build();
    }

}

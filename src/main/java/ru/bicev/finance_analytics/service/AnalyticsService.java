package ru.bicev.finance_analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;
import ru.bicev.finance_analytics.util.CategoryType;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserService userService;

    public AnalyticsService(TransactionRepository transactionRepository,
            RecurringTransactionRepository recurringTransactionRepository,
            BudgetRepository budgetRepository,
            UserService userService) {
        this.transactionRepository = transactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.budgetRepository = budgetRepository;
        this.userService = userService;
    }

    /**
     * Траты по категориям за период
     */
    public Map<String, BigDecimal> getExpensesByCategory(UUID accountId, YearMonth month) {
        List<Transaction> transactions = getTransactionsForAccountAndMonth(accountId, month);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    /**
     * Топ N категорий по тратам
     */
    public List<Map.Entry<String, BigDecimal>> getTopCategories(UUID accountId, YearMonth month, int limit) {

        Map<String, BigDecimal> expensesByCategory = getExpensesByCategory(accountId, month);

        return expensesByCategory.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .toList();
    }

    /**
     * Траты по аккаунтам
     */
    public Map<String, BigDecimal> getExpensesByAccount(UUID accountId, YearMonth month) {
        Long userId = getCurrentUserId();
        List<Transaction> transactions = transactionRepository.findAllByUserIdAndDateBetween(userId, month.atDay(1),
                month.atEndOfMonth());

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAccount().getName(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

    }

    /**
     * Траты по дням (для графиков)
     */
    public Map<LocalDate, BigDecimal> getDailyExpenses(UUID accountId, YearMonth month) {
        List<Transaction> transactions = getTransactionsForAccountAndMonth(accountId, month);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDate(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    /**
     * Траты по месяцам (MM.yyyy → total)
     */
    public Map<String, BigDecimal> getMonthlyExpenses(UUID accountId, YearMonth month) {
        List<Transaction> transactions = getTransactionsForAccountAndMonth(accountId, month);
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDate().format(DateTimeFormatter.ofPattern("MM.yyyy")),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    /**
     * Общие суммы: расходы, доходы, баланс
     */
    public Map<String, BigDecimal> getSummary(UUID accountId, YearMonth month) {
        List<Transaction> transactions = getTransactionsForAccountAndMonth(accountId, month);

        BigDecimal income = transactions.stream()
                .filter(t -> t.getCategory().getType() == CategoryType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = transactions.stream()
                .filter(t -> t.getCategory().getType() == CategoryType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = income.subtract(expense);

        return Map.of("income", income,
                "expense", expense,
                "balance", balance);

    }

    /**
     * Аналитика бюджета (сколько потрачено / какой процент)
     */
    public Map<String, Object> getCategoryBudgetStatus(UUID budgetId) {
        Long userId = getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new NotFoundException("Budget not found"));
        LocalDate start = budget.getMonth().atDay(1);
        LocalDate end = budget.getMonth().atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findAllByUserIdAndCategoryIdAndDateBetween(userId,
                budget.getCategory().getId(), start, end);

        BigDecimal spent = transactions.stream().map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentUsed = BigDecimal.ZERO;
        if (budget.getLimitAmount() != null && budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentUsed = spent.multiply(BigDecimal.valueOf(100))
                    .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);
        }

        return Map.of("category", budget.getCategory().getName(),
                "limit", budget.getLimitAmount(),
                "spent", spent,
                "percentUsed", percentUsed);
    }

    /**
     * Прогноз затрат на основе RT
     */
    public Map<String, BigDecimal> getUpcomingRecurringPayments() {
        Long userId = getCurrentUserId();
        List<RecurringTransaction> transactions = recurringTransactionRepository
                .findAllByUserIdAndActiveAndNextExecutionDateGreaterThan(userId, true, LocalDate.now());

        return transactions.stream()
                .collect(
                        Collectors.groupingBy(
                                t -> t.getNextExecutionDate().format(DateTimeFormatter.ofPattern("MM.yyyy")),
                                Collectors.mapping(RecurringTransaction::getAmount,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private List<Transaction> getTransactionsForAccountAndMonth(UUID accountId, YearMonth month) {
        Long userId = getCurrentUserId();
        return transactionRepository.findAllByUserIdAndAccountIdAndDateBetween(userId,
                accountId, month.atDay(1), month.atEndOfMonth());
    }

    private Long getCurrentUserId() {
        return userService.getCurrentUser().getId();
    }

}

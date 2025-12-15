package ru.bicev.finance_analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ru.bicev.finance_analytics.dto.CategoryBudgetStatusDto;
import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.dto.DateRange;
import ru.bicev.finance_analytics.dto.MonthlyExpenseDto;
import ru.bicev.finance_analytics.dto.RecurringForecastDto;
import ru.bicev.finance_analytics.dto.SummaryDto;
import ru.bicev.finance_analytics.dto.TopCategoryDto;
import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;
import ru.bicev.finance_analytics.util.CategoryType;

/**
 * Сервис, выполняющий аналитику расходов для текущего пользователя
 */
@Service
public class AnalyticsService {

        private final TransactionRepository transactionRepository;
        private final RecurringTransactionRepository recurringTransactionRepository;
        private final BudgetRepository budgetRepository;
        private final UserService userService;

        private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

        private static final BigDecimal ZERO = BigDecimal.ZERO;

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
         * Возвращает список категорий расходов за указанный месяц.
         * <p>
         * Категории сортируются по убыванию суммы расхода.
         * 
         * @param month месяц, за который рассчитываются расходы
         * @return список всех категорий за месяц с суммой расходов на них
         */
        public List<CategoryExpenseDto> getExpensesByCategory(YearMonth month) {
                List<Transaction> transactions = getExpensesAndMonth(month);

                logger.debug("getExpensesByCategory() for month: {}", month.toString());
                return transactions.stream()
                                .collect(Collectors.groupingBy(
                                                Transaction::getCategory,
                                                Collectors.reducing(ZERO, Transaction::getAmount,
                                                                BigDecimal::add)))
                                .entrySet()
                                .stream()
                                .map(e -> new CategoryExpenseDto(
                                                e.getKey().getName(),
                                                e.getValue().setScale(2)))
                                .sorted((e1, e2) -> e2.total().compareTo(e1.total()))
                                .toList();

        }

        /**
         * Возвращает список топ-категорий расходов за указанный месяц
         * <p>
         * Категории сортируются в порядке убывания общей суммы расходов
         * Размер возвращаемого списка ограничен параметром {@code limit}
         * 
         * @param month месяц, за который рассчитываются расходы
         * @param limit максимальное количество категорий в результате
         * @return список наиболее затратных категорий
         * @throws IllegalArgumentException если {@code limit} меньше или равен нулю
         */
        public List<TopCategoryDto> getTopCategories(YearMonth month, int limit) {

                var expensesByCategory = getExpensesByCategory(month);
                logger.debug("getTopCategories() for month: {}", month.toString());
                return expensesByCategory.stream()
                                .sorted((e1, e2) -> e2.total().compareTo(e1.total()))
                                .limit(limit)
                                .map(cat -> new TopCategoryDto(cat.category(), cat.total().setScale(2)))
                                .toList();
        }

        /**
         * Возвращает список расходов за указанный месяц по дням трат
         * 
         * @param month месяц, за который рассчитываются расходы
         * @return список трат за укзанный месяц по дням
         */
        public List<DailyExpenseDto> getDailyExpenses(YearMonth month) {
                List<Transaction> transactions = getExpensesAndMonth(month);

                logger.debug("getDailyExpenses() for month: {}", month.toString());
                return transactions.stream()
                                .collect(Collectors.groupingBy(
                                                t -> t.getDate(),
                                                Collectors.mapping(Transaction::getAmount,
                                                                Collectors.reducing(ZERO,
                                                                                BigDecimal::add))))
                                .entrySet()
                                .stream()
                                .map(e -> new DailyExpenseDto(e.getKey(), e.getValue().setScale(2)))
                                .toList();
        }

        /**
         * Возвращает список расходов за указанный период по месяцам в формате "MM.yyyy"
         * 
         * @param range временной период, за который рассчитваются расходы
         * @return спиоск трат сгруппированных по месяцам 
         * @throws IllegalStateException если дата начала периода позднее даты окончания периода
         */
        public List<MonthlyExpenseDto> getMonthlyExpenses(DateRange range) {
                if (range.start().isAfter(range.end())) {
                        throw new IllegalStateException("Start of date range can not be after end");

                }
                List<Transaction> transactions = transactionRepository
                                .findAllByUserIdAndCategory_TypeAndDateBetween(getCurrentUserId(),
                                                CategoryType.EXPENSE, range.start(), range.end());
                logger.debug("getMonthlyExpenses() for range from: {}, to: {}", range.start().toString(),
                                range.end().toString());
                return transactions.stream()
                                .collect(Collectors.groupingBy(
                                                t -> YearMonth.from(t.getDate()),
                                                Collectors.reducing(ZERO, Transaction::getAmount,
                                                                BigDecimal::add)))
                                .entrySet()
                                .stream()
                                .map(e -> new MonthlyExpenseDto(
                                                e.getKey().format(DateTimeFormatter.ofPattern("MM.yyyy")),
                                                e.getValue().setScale(2)))
                                .toList();
        }

        /**
         * Возвращает поступления, траты и баланс за указанный месяц
         * 
         * @param month месяц, за который рассчитываются транзакции
         * @return дто, в котором укзаны поступления, расходы и баланс за указанный месяц
         */
        public SummaryDto getSummary(YearMonth month) {
                List<Transaction> transactions = getTransactionsForMonth(month);

                BigDecimal income = ZERO;

                BigDecimal expense = ZERO;

                for (Transaction t : transactions) {
                        if (t.getCategory().getType() == CategoryType.INCOME) {
                                income = income.add(t.getAmount());
                        } else {

                                expense = expense.add(t.getAmount());
                        }
                }

                BigDecimal balance = income.subtract(expense);

                return new SummaryDto(income, expense, balance);
        }

        /**
         * Возвращает данные для указанного бюджета (имя категории, лимит для категории, сколько потрачено и использованный процент)
         * 
         * @param budgetId идентификатор бюджета, для которого будет произведен рассчет
         * @return дто, в котором указаны имя категории, лимит на категорию, сколько потрачено и использованный процент
         * @throws NotFoundException если передан идентификатор для не существующего бюджета
         */
        public CategoryBudgetStatusDto getCategoryBudgetStatus(UUID budgetId) {
                Long userId = getCurrentUserId();
                Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                                .orElseThrow(() -> new NotFoundException("Budget not found"));
                LocalDate start = budget.getMonth().atDay(1);
                LocalDate end = budget.getMonth().atEndOfMonth();

                logger.debug("getCategoryBudgetStatus() for budget: {}", budgetId.toString());

                List<Transaction> transactions = transactionRepository.findAllByUserIdAndCategoryIdAndDateBetween(
                                userId,
                                budget.getCategory().getId(), start, end);

                BigDecimal spent = transactions.stream()
                                .map(Transaction::getAmount)
                                .reduce(ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal percentUsed = ZERO;
                if (budget.getLimitAmount() != null && budget.getLimitAmount().compareTo(ZERO) > 0) {
                        percentUsed = spent
                                        .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(2, RoundingMode.HALF_UP);
                }

                return new CategoryBudgetStatusDto(
                                budget.getCategory().getName(),
                                budget.getLimitAmount(),
                                spent,
                                percentUsed);
        }

        /**
         * Возвращает список пронозируемых трат на основе рекуррентных платежей в формате "MM.yyyy"
         * 
         * @return список пргнозируемых расходов за месяц
         */
        public List<RecurringForecastDto> getUpcomingRecurringPayments() {
                Long userId = getCurrentUserId();
                List<RecurringTransaction> transactions = recurringTransactionRepository
                                .findAllByUserIdAndActiveAndNextExecutionDateGreaterThan(userId, true, LocalDate.now());

                logger.debug("getUpcomintRecurringPayments() for user: {}", userId);

                return transactions.stream()
                                .collect(
                                                Collectors.groupingBy(
                                                                t -> t.getNextExecutionDate().format(
                                                                                DateTimeFormatter.ofPattern("MM.yyyy")),
                                                                Collectors.mapping(RecurringTransaction::getAmount,
                                                                                Collectors.reducing(ZERO,
                                                                                                BigDecimal::add))))
                                .entrySet()
                                .stream()
                                .map(e -> new RecurringForecastDto(e.getKey(), e.getValue().setScale(2))).toList();
        }

        /**
         * Служебный метод, возвращающий список транзакций за указанный месяц
         * 
         * @param month месяц, за который получаются транзакции
         * @return список транзакций за указанный месяц
         */
        private List<Transaction> getTransactionsForMonth(YearMonth month) {
                Long userId = getCurrentUserId();
                return transactionRepository.findAllByUserIdAndDateBetween(userId,
                                month.atDay(1), month.atEndOfMonth());
        }

        /**
         * Служебный метод, возвращающий список расходов за указанный месяц
         * 
         * @param month месяц, за который получаются транзакции
         * @return список расходов, за указанный месяц
         */
        private List<Transaction> getExpensesAndMonth(YearMonth month) {
                Long userId = getCurrentUserId();
                return transactionRepository.findAllByUserIdAndCategory_TypeAndDateBetween(userId,
                                CategoryType.EXPENSE, month.atDay(1), month.atEndOfMonth());
        }

        /**
         * Служебный метод, возвращающий идентификатор текущего пользователя
         * 
         * @return возвращает идентификатор текущего пользователя
         */
        private Long getCurrentUserId() {
                return userService.getCurrentUser().getId();
        }

}

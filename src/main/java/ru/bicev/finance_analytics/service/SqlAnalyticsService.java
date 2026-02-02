package ru.bicev.finance_analytics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.dto.MonthlyExpenseDto;
import ru.bicev.finance_analytics.dto.SummaryDto;
import ru.bicev.finance_analytics.dto.TopCategoryDto;
import ru.bicev.finance_analytics.repo.TransactionAnalyticsRepository;

/**
 * Альтернативный сервис аналитики, который выполняет аналитику на уровне бд, а не приложения
 */
@Service
public class SqlAnalyticsService {

    private final TransactionAnalyticsRepository transactionRepository;
    private final UserService userService;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.yyyy");

    public SqlAnalyticsService(TransactionAnalyticsRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    /**
     * Возвращает список трат по категориям за указанный месяц
     * @param month месяц, за который ищутся расходы
     * @return список расходов за указанный месяц в порядке убывания суммы трат
     */
    public List<CategoryExpenseDto> getCategoryExpenses(YearMonth month) {
        return transactionRepository.getExpensesByCategory(getCurrentUserId(), month.atDay(1), month.atEndOfMonth());
    }

    /**
     * Возвращает список топ-категорий расходов за указанный месяц
     * @param month месяц, за который ищутся расходы
     * @param limit максимальное количество категорий в выдаче 
     * @return список наиболее затратных категорий
     * @throws IllegalArgumentExeption если {@code limit} меньше или равен нулю
     */
    public List<TopCategoryDto> getTopCategories(YearMonth month, int limit) {
        return transactionRepository.getTopCategories(getCurrentUserId(), month.atDay(1), month.atEndOfMonth(), limit)
                .stream()
                .map(projection -> new TopCategoryDto(
                        projection.getCategoryName(),
                        projection.getTotalAmount()))
                .toList();
    }

    /**
     * Возвращает список расходов за месяц по дням
     * @param month месяц, за который ищутся расходы
     * @return список расходов по дням
     */
    public List<DailyExpenseDto> getDailyExpenses(YearMonth month) {
        return transactionRepository.getDailyExpenses(getCurrentUserId(), month.atDay(1), month.atEndOfMonth());
    }

    /**
     * Возвращает список расходов за указанный период по месяцам
     * @param start дата начала периода
     * @param end дата конца периода
     * @return список расходов по месяцам
     * @throws IllegalArgumentException если {@code start} идет после {@code end} хронологически
     */
    public List<MonthlyExpenseDto> getMonthlyExpenses(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalStateException("Start cannot be after end");
        }
        return transactionRepository.getMonthlyExpenses(getCurrentUserId(), start, end)
                .stream()
                .map(projection -> new MonthlyExpenseDto(
                        projection.getMonth().format(FORMAT),
                        projection.getTotalAmount()))
                .toList();
    }

    /**
     * Возвращает поступления, траты и баланс за указанный месяц
     * @param month месяц, для рассчета поступлений, трат и баланса
     * @return дто, содержащее поступления, траты и баланс за указанный месяц
     */
    public SummaryDto getSummary(YearMonth month) {
        var projection = transactionRepository.getSummary(getCurrentUserId(), month.atDay(1), month.atEndOfMonth());

        var income = projection.getIncome() != null ? projection.getIncome() : BigDecimal.ZERO;
        var expense = projection.getExpense() != null ? projection.getExpense() : BigDecimal.ZERO;

        return new SummaryDto(income, expense, income.subtract(expense));
    }

    /**
     * Служебный метод, который возвращает идентификатор текущего пользователя
     * @return идентификатор текущего пользователя
     */
    private Long getCurrentUserId() {
        return userService.getCurrentUserId();
    }

}

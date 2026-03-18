package ru.bicev.finance_analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.CategoryBudgetStatusDto;
import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.dto.DateRange;
import ru.bicev.finance_analytics.dto.MonthlyExpenseDto;
import ru.bicev.finance_analytics.dto.RecurringForecastDto;
import ru.bicev.finance_analytics.dto.SummaryDto;
import ru.bicev.finance_analytics.dto.TopCategoryDto;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.JooqAnalyticsRepository;

/**
 * Сервис, выполняющий аналитику расходов для текущего пользователя
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

        private final JooqAnalyticsRepository jooqRepo;
        private final UserService userService;

        private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

        public AnalyticsService(JooqAnalyticsRepository jooqRepo,
                        UserService userService) {
                this.jooqRepo = jooqRepo;
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

                logger.debug("getExpensesByCategory() for month: {}", month.toString());
                Long userId = getCurrentUserId();
                return jooqRepo.getExpensesByCategory(userId, month.atDay(1), month.atEndOfMonth());

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
                if (limit <= 0) {
                        throw new IllegalArgumentException("Limit can not be less than 1");
                }
                logger.debug("getTopCategories() for month: {}", month.toString());
                Long userId = getCurrentUserId();
                return jooqRepo.getTopCategories(userId, month.atDay(1), month.atEndOfMonth(), limit);
        }

        /**
         * Возвращает список расходов за указанный месяц по дням трат
         * 
         * @param month месяц, за который рассчитываются расходы
         * @return список трат за укзанный месяц по дням
         */
        public List<DailyExpenseDto> getDailyExpenses(YearMonth month) {
                logger.debug("getDailyExpenses() for month: {}", month.toString());
                Long userId = getCurrentUserId();
                return jooqRepo.getDailyExpenses(userId, month.atDay(1), month.atEndOfMonth());
        }

        /**
         * Возвращает список расходов за указанный период по месяцам в формате "MM.yyyy"
         * 
         * @param range временной период, за который рассчитваются расходы
         * @return спиоск трат сгруппированных по месяцам
         * @throws IllegalStateException если дата начала периода позднее даты окончания
         *                               периода
         */
        public List<MonthlyExpenseDto> getMonthlyExpenses(DateRange range) {
                if (range.start().isAfter(range.end())) {
                        throw new IllegalStateException("Start of date range can not be after end");

                }
                logger.debug("getMonthlyExpenses() for range from: {}, to: {}", range.start().toString(),
                                range.end().toString());
                Long userId = getCurrentUserId();
                return jooqRepo.getMonthlyExpenses(userId, range.start(), range.end());

        }

        /**
         * Возвращает поступления, суммы лититов бюджетов, траты и баланс за указанный
         * месяц
         * 
         * @param month месяц, за который рассчитываются транзакции
         * @return дто, в котором укзаны поступления, расходы и баланс за указанный
         *         месяц
         */
        public SummaryDto getSummary(YearMonth month) {
                logger.debug("getSummary() for month: {}", month.toString());
                Long userId = getCurrentUserId();
                return jooqRepo.getSummaryForMonth(userId, month.atDay(1), month.atEndOfMonth());
        }

        /**
         * Возвращает данные для указанного бюджета (имя категории, лимит для категории,
         * сколько потрачено и использованный процент)
         * 
         * @param budgetId идентификатор бюджета, для которого будет произведен рассчет
         * @return дто, в котором указаны имя категории, лимит на категорию, сколько
         *         потрачено и использованный процент
         * @throws NotFoundException если передан идентификатор для не существующего
         *                           бюджета
         */
        public CategoryBudgetStatusDto getCategoryBudgetStatus(UUID budgetId) {
                Long userId = getCurrentUserId();
                logger.debug("getCategoryBudgetStatus() for budget: {}", budgetId.toString());

                var dto = jooqRepo.getCategoryBudgetStatus(userId, budgetId).orElseThrow(
                                () -> new NotFoundException("Budget not found for id: " + budgetId.toString()));

                return dto.normilize();
        }

        /**
         * Возвращает список пронозируемых трат на основе рекуррентных платежей в
         * формате "MM.yyyy"
         * 
         * @return список пргнозируемых расходов за месяц
         */
        public List<RecurringForecastDto> getUpcomingRecurringPayments() {
                Long userId = getCurrentUserId();
                logger.debug("getUpcomintRecurringPayments() for user: {}", userId);

                return jooqRepo.getUpcomingPayments(userId);
        }

        /**
         * Служебный метод, возвращающий идентификатор текущего пользователя
         * 
         * @return возвращает идентификатор текущего пользователя
         */
        private Long getCurrentUserId() {
                return userService.getCurrentUserId();
        }

}

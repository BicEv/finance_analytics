package ru.bicev.finance_analytics.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import ru.bicev.finance_analytics.dto.CategoryBudgetStatusDto;
import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.dto.MonthlyExpenseDto;
import ru.bicev.finance_analytics.dto.RecurringForecastDto;
import ru.bicev.finance_analytics.dto.SummaryDto;
import ru.bicev.finance_analytics.dto.TopCategoryDto;
import ru.bicev.finance_analytics.util.CategoryType;

import static ru.bicev.finance_analytics.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class JooqAnalyticsRepository {

        private final DSLContext dsl;
        private static final BigDecimal ZERO = BigDecimal.ZERO;

        public List<CategoryExpenseDto> getExpensesByCategory(Long userId, LocalDate start, LocalDate end) {
                return dsl.select(
                                CATEGORIES.NAME.as("category"),
                                sum(TRANSACTIONS.AMOUNT).as("total"))
                                .from(TRANSACTIONS)
                                .join(CATEGORIES).on(TRANSACTIONS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(TRANSACTIONS.DATE.between(start, end))
                                .groupBy(CATEGORIES.NAME)
                                .orderBy(field("total").desc())
                                .fetchInto(CategoryExpenseDto.class);
        }

        public List<TopCategoryDto> getTopCategories(Long userId, LocalDate start, LocalDate end, int limit) {
                return dsl.select(
                                CATEGORIES.NAME.as("category"),
                                sum(TRANSACTIONS.AMOUNT).as("total"))
                                .from(TRANSACTIONS)
                                .join(CATEGORIES).on(TRANSACTIONS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(TRANSACTIONS.DATE.between(start, end))
                                .groupBy(CATEGORIES.NAME)
                                .orderBy(field("total").desc())
                                .limit(limit)
                                .fetchInto(TopCategoryDto.class);
        }

        public List<DailyExpenseDto> getDailyExpenses(Long userId, LocalDate start, LocalDate end) {
                return dsl.select(
                                TRANSACTIONS.DATE,
                                sum(TRANSACTIONS.AMOUNT).as("amount"))
                                .from(TRANSACTIONS)
                                .join(CATEGORIES).on(TRANSACTIONS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(TRANSACTIONS.DATE.between(start, end))
                                .and(CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name()))
                                .groupBy(TRANSACTIONS.DATE)
                                .orderBy(TRANSACTIONS.DATE.asc())
                                .fetchInto(DailyExpenseDto.class);

        }

        public List<MonthlyExpenseDto> getMonthlyExpenses(Long userId, LocalDate start, LocalDate end) {
                var monthField = toChar(TRANSACTIONS.DATE, "MM-YYYY").as("month");
                var monthRaw = field("date_trunc('month', {0})", SQLDataType.LOCALDATE, TRANSACTIONS.DATE);
                return dsl.select(
                                monthField,
                                sum(TRANSACTIONS.AMOUNT).as("total"))
                                .from(TRANSACTIONS)
                                .join(CATEGORIES).on(TRANSACTIONS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name()))
                                .and(TRANSACTIONS.DATE.between(start, end))
                                .groupBy(monthField, monthRaw)
                                .orderBy(monthRaw.asc())
                                .fetchInto(MonthlyExpenseDto.class);
        }

        public SummaryDto getSummaryForMonth(Long userId, LocalDate start, LocalDate end) {
                var budgetSum = dsl.select(coalesce(sum(BUDGETS.AMOUNT), ZERO))
                                .from(BUDGETS)
                                .where(BUDGETS.USER_ID.eq(userId))
                                .and(BUDGETS.MONTH.cast(SQLDataType.LOCALDATE).between(start, end))
                                .asField();

                var incomeTrSum = dsl.select(coalesce(sum(TRANSACTIONS.AMOUNT), ZERO))
                                .from(TRANSACTIONS)
                                .join(CATEGORIES).on(TRANSACTIONS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(CATEGORIES.TYPE.eq(CategoryType.INCOME.name()))
                                .and(TRANSACTIONS.DATE.between(start, end))
                                .asField();

                var expenseTrSum = dsl.select(coalesce(sum(TRANSACTIONS.AMOUNT), ZERO))
                                .from(TRANSACTIONS)
                                .join(CATEGORIES).on(TRANSACTIONS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name()))
                                .and(TRANSACTIONS.DATE.between(start, end))
                                .asField();

                return dsl.select(
                                budgetSum.plus(incomeTrSum).as("income"), // Лимиты + Реальные доходы
                                expenseTrSum.as("expense"),
                                budgetSum.plus(incomeTrSum).minus(expenseTrSum).as("balance"))
                                .fetchOneInto(SummaryDto.class);
        }

        public Optional<CategoryBudgetStatusDto> getCategoryBudgetStatus(Long userId, UUID budgetId) {
                var limit = BUDGETS.AMOUNT;

                var spentExpr = select(coalesce(sum(TRANSACTIONS.AMOUNT), ZERO))
                                .from(TRANSACTIONS)
                                .where(TRANSACTIONS.USER_ID.eq(userId))
                                .and(TRANSACTIONS.CATEGORY_ID.eq(BUDGETS.CATEGORY_ID))
                                .and(field("date_trunc('month', {0})", SQLDataType.LOCALDATE, TRANSACTIONS.DATE)
                                                .eq(BUDGETS.MONTH.cast(SQLDataType.LOCALDATE)))
                                .asField();
                var percent = coalesce(
                                spentExpr.div(nullif(limit, ZERO)).mul(100),
                                ZERO).as("percentUsed");

                return dsl.select(
                                CATEGORIES.NAME.as("category"),
                                BUDGETS.AMOUNT.as("limit"),
                                spentExpr.as("spent"),
                                percent)
                                .from(BUDGETS)
                                .join(CATEGORIES).on(BUDGETS.CATEGORY_ID.eq(CATEGORIES.ID))
                                .where(BUDGETS.ID.eq(budgetId))
                                .and(BUDGETS.USER_ID.eq(userId))
                                .fetchOptionalInto(CategoryBudgetStatusDto.class);

        }

        public List<RecurringForecastDto> getUpcomingPayments(Long userId) {
                var monthField = toChar(RECURRINGS.NEXT_EXECUTION_DATE, "MM-YYYY").as("month");
                var monthRaw = field("date_trunc('month', {0})", SQLDataType.LOCALDATE, RECURRINGS.NEXT_EXECUTION_DATE);
                return dsl.select(
                                monthField,
                                sum(RECURRINGS.AMOUNT).as("expectedAmount"))
                                .from(RECURRINGS)
                                .where(RECURRINGS.USER_ID.eq(userId))
                                .and(RECURRINGS.IS_ACTIVE.isTrue())
                                .and(RECURRINGS.NEXT_EXECUTION_DATE.ge(LocalDate.now()))
                                .groupBy(monthField, monthRaw)
                                .orderBy(monthRaw.asc())
                                .fetchInto(RecurringForecastDto.class);

        }

}

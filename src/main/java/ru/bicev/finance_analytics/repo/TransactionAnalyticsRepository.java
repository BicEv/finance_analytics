package ru.bicev.finance_analytics.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.repo.projection.MonthlyExpenseProjection;
import ru.bicev.finance_analytics.repo.projection.SummaryProjection;
import ru.bicev.finance_analytics.repo.projection.TopCategoryProjection;

@org.springframework.stereotype.Repository
public interface TransactionAnalyticsRepository extends Repository<Transaction, UUID> {

    @Query("""
            SELECT new ru.bicev.finance_analytics.dto.CategoryExpenseDto(
            t.category.name,
            SUM(t.amount)
            )
            FROM Transaction t
            WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end
            GROUP BY t.category.name
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryExpenseDto> getExpensesByCategory(Long userId, LocalDate start, LocalDate end);

    @Query(value = """
            SELECT
            c.name AS categoryName,
            SUM(t.amount) AS totalAmount
            FROM transaction t
            JOIN сategory c ON t.category_id = c.id
            WHERE t.user_id = :userId
            AND t.date BETWEEN :start AND :end
            GROUP BY c.name
            ORDER BY totalAmount DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopCategoryProjection> getTopCategories(Long userId, LocalDate start, LocalDate end, int limit);

    @Query("""
            SELECT new ru.bicev.finance_analytics.dto.DailyExpenseDto(
            t.date,
            SUM(t.amount)
            )
            FROM Transaction t
            WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end
            GROUP BY t.date
            ORDER BY t.date ASC
            """)
    List<DailyExpenseDto> getDailyExpenses(Long userId, LocalDate start, LocalDate end);

    @Query(value = """
            SELECT
            DATE_TRUNC('month', t.date) AS month,
            SUM(t.amount) AS totalAmount
            FROM transaction t
            WHERE t.user_id = :userId
            AND t.date BETWEEN :start AND :end
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<MonthlyExpenseProjection> getMonthlyExpenses(Long userId, LocalDate start, LocalDate end);

    @Query(value = """
            SELECT
            SUM(CASE WHEN t.type = 'INCOME' then t.amount ELSE 0 END)::numeric AS income,
            SUM(CASE WHEN t.type = 'EXPENSE' then t.amount ELSE 0 END)::numeric AS expense
            FROM transaction t
            WHERE t.user_id = :userId
            AND t.date BETWEEN :start AND :end
            """, nativeQuery = true)
    SummaryProjection getSummary(Long userId, LocalDate start, LocalDate end);

}

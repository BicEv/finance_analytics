package ru.bicev.finance_analytics.repo;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.bicev.finance_analytics.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findAllByUserId(Long userId);

    List<Budget> findByUserIdAndMonth(Long userId, YearMonth month);

    @Query("SELECT SUM(b.limitAmount) from Budget b where b.user.id = :userId and b.month = :month")
    Optional<BigDecimal> sumLimitAmountByUserIdAndMonth(Long userId, YearMonth month);

    Optional<Budget> findByIdAndUserId(UUID id, Long userId);

    Optional<Budget> findByUserIdAndCategoryIdAndMonth(Long userId, UUID categoryId, YearMonth month);

    boolean existsByUserIdAndCategoryIdAndMonth(Long userId, UUID categoryId, YearMonth month);

}

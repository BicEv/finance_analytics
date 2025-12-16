package ru.bicev.finance_analytics.repo;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findAllByUserId(Long userId);

    List<Budget> findByUserIdAndMonth(Long userId, YearMonth month);

    Optional<Budget> findByIdAndUserId(UUID id, Long userId);

    Optional<Budget> findByUserIdAndCategoryIdAndMonth(Long userId, UUID categoryId, YearMonth month);

    boolean existsByUserIdAndCategoryIdAndMonth(Long userId, UUID categoryId, YearMonth month);

}

package ru.bicev.finance_analytics.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.BudgetTemplate;

public interface BudgetTemplateRepository extends JpaRepository<BudgetTemplate, UUID> {

    List<BudgetTemplate> findActiveTrue();

    List<BudgetTemplate> findAllByUserId(Long userId);

    Optional<BudgetTemplate> findByIdAndUserId(UUID id, Long userId);

    boolean existsByUserIdAndCategoryId(Long userId, UUID categoryId);

}

package ru.bicev.finance_analytics.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.util.CategoryType;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUserId(Long userId);

    List<Category> findAllByUserIdAndAccountId(Long userId, UUID accountId);

    List<Category> findAllByUserIdAndType(Long userId, CategoryType type);

    Optional<Category> findByIdAndUserId(UUID id, Long userId);

}

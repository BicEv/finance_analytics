package ru.bicev.finance_analytics.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUserId(Long userId);

    Optional<Category> findByIdAndUserId(UUID id, Long userId);

}

package ru.bicev.finance_analytics.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.util.CategoryType;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

        List<Transaction> findAllByUserId(Long userId);

        List<Transaction> findAllByUserIdAndCategoryId(Long userId, UUID categoryId);

        List<Transaction> findAllByUserIdAndCategoryIdAndDateBetween(Long userId, UUID categoryId, LocalDate start,
                        LocalDate end);

        List<Transaction> findAllByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

        List<Transaction> findAllByUserIdAndCategory_TypeAndDateBetween(Long userId,
                        CategoryType type, LocalDate start, LocalDate end);

        Optional<Transaction> findByIdAndUserId(UUID id, Long userId);

}

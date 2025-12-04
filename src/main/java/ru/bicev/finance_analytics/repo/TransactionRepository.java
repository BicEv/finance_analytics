package ru.bicev.finance_analytics.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUserId(Long userId);

    List<Transaction> findAllByUserIdAndAccountId(Long userId, UUID accountId);

    List<Transaction> findAllByUserIdAndCategoryId(Long userId, UUID categoryId);

    List<Transaction> findAllByUserIdAndCategoryIdAndDateBetween(Long userId, UUID categoryId, LocalDate start,
            LocalDate end);

    List<Transaction> findAllByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findAllByUserIdAndAccountIdAndDateBetween(Long userId, UUID accountId, LocalDate start,
            LocalDate end);

    Optional<Transaction> findByIdAndUserId(UUID id, Long userId);

}

package ru.bicev.finance_analytics.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.RecurringTransaction;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    List<RecurringTransaction> findAllByUserId(Long userId);

    List<RecurringTransaction> findAllByUserIdAndNextExecutionDateLessThanEqual(Long userId, LocalDate date);

    List<RecurringTransaction> findAllByActiveAndNextExecutionDateLessThanEqual(boolean active, LocalDate date);

    List<RecurringTransaction> findAllByUserIdAndActiveAndNextExecutionDateGreaterThan(Long userId, boolean active,
            LocalDate date);

    Optional<RecurringTransaction> findByIdAndUserId(UUID id, Long userId);

}

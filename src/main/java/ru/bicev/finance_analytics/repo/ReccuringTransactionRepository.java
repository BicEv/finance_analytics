package ru.bicev.finance_analytics.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.ReccuringTransaction;

public interface ReccuringTransactionRepository extends JpaRepository<ReccuringTransaction, UUID> {

    List<ReccuringTransaction> findAllByUserId(Long userId);

    List<ReccuringTransaction> findAllByUserIdAndNextExecutionDateLessThanEqual(Long userId, LocalDate date);

    Optional<ReccuringTransaction> findByIdAndUserId(UUID id, Long userId);

}

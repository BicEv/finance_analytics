package ru.bicev.finance_analytics.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByUserId(Long userId);

    Optional<Account> findByIdAndUserId(UUID id, Long userId);

}

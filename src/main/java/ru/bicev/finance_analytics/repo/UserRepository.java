package ru.bicev.finance_analytics.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.bicev.finance_analytics.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByEmail(String email);

}

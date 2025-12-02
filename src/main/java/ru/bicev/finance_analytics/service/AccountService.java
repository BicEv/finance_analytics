package ru.bicev.finance_analytics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.CreateAccountRequest;
import ru.bicev.finance_analytics.entity.Account;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.AccountRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    public AccountService(AccountRepository accountRepository, UserService userService) {
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    public List<Account> getUserAccounts() {
        Long userId = getCurrentUser().getId();
        return accountRepository.findAllByUserId(userId);
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .user(getCurrentUser())
                .name(request.name())
                .currency(request.currency())
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    public Account getAccountById(UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    @Transactional
    public Account updateAccount(UUID accountId, String name) {
        Account account = accountRepository.findByIdAndUserId(accountId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        account.setName(name);
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(UUID accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        accountRepository.delete(account);
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

}

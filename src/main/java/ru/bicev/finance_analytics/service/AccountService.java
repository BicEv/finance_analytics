package ru.bicev.finance_analytics.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.AccountDto;
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

    public List<AccountDto> getUserAccounts() {
        Long userId = getCurrentUser().getId();
        return accountRepository.findAllByUserId(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .user(getCurrentUser())
                .name(request.name())
                .currency(request.currency())
                .createdAt(LocalDateTime.now())
                .build();

        return toDto(accountRepository.save(account));
    }

    public AccountDto getAccountById(UUID accountId) {
        Account acc = accountRepository.findByIdAndUserId(accountId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return toDto(acc);
    }

    @Transactional
    public AccountDto updateAccount(UUID accountId, String name) {
        Account account = accountRepository.findByIdAndUserId(accountId, getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        account.setName(name);
        return toDto(accountRepository.save(account));
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

    private AccountDto toDto(Account account) {
        return new AccountDto(account.getId(), account.getName(), account.getCurrency());
    }

}

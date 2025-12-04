package ru.bicev.finance_analytics.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.RecurringTransactionRequest;
import ru.bicev.finance_analytics.entity.Account;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.AccountRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;

@Service
public class RecuringTransactionService {

    private final UserService userService;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public RecuringTransactionService(UserService userService,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository, RecurringTransactionRepository recurringTransactionRepository) {
        this.userService = userService;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    @Transactional
    public RecurringTransaction createTransaction(RecurringTransactionRequest request) {
        User user = getCurrentUser();
        Account account = getAccount(request.accountId(), user.getId());
        Category category = getCategory(request.categoryId(), user.getId());

        if (request.nextExecutionDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Next execution date cannot be before current date");
        }

        RecurringTransaction transaction = RecurringTransaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .frequency(request.frequency())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .isActive(request.isActive())
                .nextExecutionDate(request.nextExecutionDate())
                .build();

        return recurringTransactionRepository.save(transaction);
    }

    public List<RecurringTransaction> getAllReccuringTransactions() {
        Long userId = getCurrentUserId();

        return recurringTransactionRepository.findAllByUserId(userId);
    }

    public List<RecurringTransaction> getAllReccuringTransactionsAndDate(LocalDate date) {
        Long userId = getCurrentUserId();

        return recurringTransactionRepository.findAllByUserIdAndNextExecutionDateLessThanEqual(userId, date);
    }

    public RecurringTransaction getTransactionById(UUID transactionId) {

        return recurringTransactionRepository.findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    @Transactional
    public RecurringTransaction updateTransaction(UUID transactionId, RecurringTransactionRequest request) {
        Long userId = getCurrentUserId();
        RecurringTransaction transaction = recurringTransactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (request.accountId() != null) {
            transaction.setAccount(getAccount(request.accountId(), userId));
        }

        if (request.categoryId() != null) {
            transaction.setCategory(getCategory(request.categoryId(), userId));
        }

        if (request.amount() != null) {
            transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.description() != null) {
            transaction.setDescription(request.description());
        }

        if (request.frequency() != null) {
            transaction.setFrequency(request.frequency());
        }

        if (request.nextExecutionDate() != null) {
            if (request.nextExecutionDate().isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Next execution date cannot be before current date");
            transaction.setNextExecutionDate(request.nextExecutionDate());
        }

        transaction.setActive(request.isActive());

        return recurringTransactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID transactionId) {
        RecurringTransaction transaction = recurringTransactionRepository
                .findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        recurringTransactionRepository.delete(transaction);
    }

    public List<RecurringTransaction> findAllActiveByNextExecutionDateBefore(LocalDate now) {
        return recurringTransactionRepository.findAllByActiveAndNextExecutionDateLessThanEqual(true, now);
    }

    @Transactional
    public RecurringTransaction save(RecurringTransaction transaction) {
        return recurringTransactionRepository.save(transaction);
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    private Long getCurrentUserId() {
        return userService.getCurrentUser().getId();
    }

    private Account getAccount(UUID accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private Category getCategory(UUID categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

}

package ru.bicev.finance_analytics.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.TransactionRequest;
import ru.bicev.finance_analytics.entity.Account;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.AccountRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;

@Service
public class TransactionService {

    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(UserService userService, TransactionRepository transactionRepository,
            AccountRepository accountRepository, CategoryRepository categoryRepository) {
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Transaction createTransaction(TransactionRequest request) {
        User user = getCurrentUser();
        Account account = getAccount(request.accountId(), user.getId());
        Category category = getCategory(request.categoryId(), user.getId());

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .date(request.date() != null ? request.date() : LocalDate.now())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .createdAt(LocalDateTime.now())
                .description(request.description())
                .isPlanned(request.isPlanned())
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction createTransactionForUser(User user, TransactionRequest request) {
        Account account = getAccount(request.accountId(), user.getId());
        Category category = getCategory(request.categoryId(), user.getId());

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .category(category)
                .date(request.date() != null ? request.date() : LocalDate.now())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .createdAt(LocalDateTime.now())
                .description(request.description())
                .isPlanned(request.isPlanned())
                .build();

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactions() {
        return transactionRepository.findAllByUserId(getCurrentUserId());
    }

    public List<Transaction> getTransactionsByDateBetween(UUID accountId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From-date cannot be after to-date");
        }

        return transactionRepository.findAllByUserIdAndAccountIdAndDateBetween(
                getCurrentUserId(),
                accountId, from, to);
    }

    public Transaction getTransactionById(UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }

    @Transactional
    public Transaction updateTransaction(UUID transactionId, TransactionRequest request) {
        Long userId = getCurrentUserId();

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (request.amount() != null) {
            transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.date() != null) {
            transaction.setDate(request.date());
        }

        transaction.setPlanned(request.isPlanned());

        if (request.description() != null) {
            transaction.setDescription(request.description());
        }

        if (request.categoryId() != null) {
            Category category = getCategory(request.categoryId(), userId);
            transaction.setCategory(category);
        }

        if (request.accountId() != null) {
            Account account = getAccount(request.accountId(), userId);
            transaction.setAccount(account);
        }

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID transactionId) {

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        transactionRepository.delete(transaction);
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

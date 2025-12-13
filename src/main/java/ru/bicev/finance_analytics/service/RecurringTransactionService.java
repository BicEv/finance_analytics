package ru.bicev.finance_analytics.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.RecurringTransactionDto;
import ru.bicev.finance_analytics.dto.RecurringTransactionRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;

@Service
public class RecurringTransactionService {

    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    private static final Logger logger = LoggerFactory.getLogger(RecurringTransactionService.class);

    public RecurringTransactionService(UserService userService,
            CategoryRepository categoryRepository, RecurringTransactionRepository recurringTransactionRepository) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    @Transactional
    public RecurringTransactionDto createTransaction(RecurringTransactionRequest request) {
        User user = getCurrentUser();
        Category category = getCategory(request.categoryId(), user.getId());

        if (request.nextExecutionDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Next execution date cannot be before current date");
        }

        RecurringTransaction transaction = RecurringTransaction.builder()
                .user(user)
                .category(category)
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .frequency(request.frequency())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .isActive(request.isActive())
                .nextExecutionDate(request.nextExecutionDate())
                .build();
        logger.debug("createRecurringTransaction() for user: {}", user.getId());
        return toDto(recurringTransactionRepository.save(transaction));
    }

    public List<RecurringTransactionDto> getAllRecurringTransactions() {
        Long userId = getCurrentUserId();
        logger.debug("getAllRecurringTransactions() for user: {}", userId);
        return recurringTransactionRepository.findAllByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<RecurringTransactionDto> getAllRecurringTransactionsAndDate(LocalDate date) {
        Long userId = getCurrentUserId();

        logger.debug("getAllRecurringTransactionsAndDate() for user: {}; date: {}", userId, date.toString());
        return recurringTransactionRepository.findAllByUserIdAndNextExecutionDateLessThanEqual(userId, date).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RecurringTransactionDto getTransactionById(UUID transactionId) {

        RecurringTransaction transaction = recurringTransactionRepository
                .findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        logger.debug("getRecurringTransactionById() with id: {}", transactionId);
        return toDto(transaction);
    }

    @Transactional
    public RecurringTransactionDto updateTransaction(UUID transactionId, RecurringTransactionRequest request) {
        Long userId = getCurrentUserId();
        RecurringTransaction transaction = recurringTransactionRepository
                .findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

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
        logger.debug("updateRecurringTransaction() with id: {}", transactionId.toString());
        transaction.setActive(request.isActive());

        return toDto(recurringTransactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(UUID transactionId) {
        RecurringTransaction transaction = recurringTransactionRepository
                .findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        logger.debug("deleteRecurringTransaction() with id: {}", transactionId.toString());
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

    private Category getCategory(UUID categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private RecurringTransactionDto toDto(RecurringTransaction transaction) {
        return new RecurringTransactionDto(
                transaction.getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getAmount(),
                transaction.getFrequency().name(),
                transaction.getDescription(),
                transaction.getNextExecutionDate(),
                transaction.isActive());
    }

}

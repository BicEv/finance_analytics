package ru.bicev.finance_analytics.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.TransactionRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.util.Frequency;

@Service
public class RecurringExecutionService {

    private final TransactionService transactionService;
    private final RecurringTransactionService recuringTransactionService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public RecurringExecutionService(TransactionService transactionService,
            RecurringTransactionService recuringTransactionService, UserRepository userRepository,
            CategoryRepository categoryRepository) {
        this.transactionService = transactionService;
        this.recuringTransactionService = recuringTransactionService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void executeDueTransactions() {
        LocalDate today = LocalDate.now();

        List<RecurringTransaction> due = recuringTransactionService.findAllActiveByNextExecutionDateBefore(today);

        for (RecurringTransaction rt : due) {
            
            Long userId = rt.getUser().getId();
            User user = userRepository.findById(rt.getUser().getId())
                    .orElseThrow(() -> new NotFoundException("User not found"));
            
            Category category = categoryRepository.findByIdAndUserId(rt.getCategory().getId(), userId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));

            TransactionRequest t = new TransactionRequest(
                    category.getId(),
                    rt.getAmount(),
                    today,
                    rt.getDescription(),
                    false);

            transactionService.createTransactionForUser(user, t);

            rt.setLastExecutionDate(today);
            rt.setNextExecutionDate(calculateNextDate(today, rt.getFrequency()));

            recuringTransactionService.save(rt);
        }
    }

    private LocalDate calculateNextDate(LocalDate current, Frequency frequency) {
        return switch (frequency) {
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }

}

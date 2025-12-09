package ru.bicev.finance_analytics;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.bicev.finance_analytics.entity.Account;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.repo.AccountRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.UserRepository;
import ru.bicev.finance_analytics.service.RecurringExecutionService;
import ru.bicev.finance_analytics.service.RecurringTransactionService;
import ru.bicev.finance_analytics.service.TransactionService;
import ru.bicev.finance_analytics.util.Frequency;

@ExtendWith(MockitoExtension.class)
public class RecurringExecutionServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private RecurringTransactionService recuringTransactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RecurringExecutionService service;

    private User user;
    private Account account;
    private Category category;
    private RecurringTransaction recurring;

    @BeforeEach
    void init() {
        user = User.builder().id(1L).build();
        account = Account.builder().id(UUID.randomUUID()).user(user).build();
        category = Category.builder().id(UUID.randomUUID()).user(user).build();

        recurring = RecurringTransaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .account(account)
                .category(category)
                .amount(BigDecimal.valueOf(100))
                .description("Test")
                .frequency(Frequency.MONTHLY)
                .isActive(true)
                .nextExecutionDate(LocalDate.now())
                .build();
    }

    @Test
    void testExecuteDueTransactions_success() {
        when(recuringTransactionService.findAllActiveByNextExecutionDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(recurring));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(account.getId(), user.getId())).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.of(category));
        when(recuringTransactionService.save(any(RecurringTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.executeDueTransactions();

        ArgumentCaptor<UUID> accountCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> categoryCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(transactionService, times(1)).createTransactionForUser(eq(user), any());

        assertEquals(LocalDate.now().plusMonths(1), recurring.getNextExecutionDate());
        assertEquals(LocalDate.now(), recurring.getLastExecutionDate());

        verify(recuringTransactionService, times(1)).save(recurring);
    }

    @Test
    void testExecuteDueTransactions_noDueTransactions() {
        when(recuringTransactionService.findAllActiveByNextExecutionDateBefore(any(LocalDate.class)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.executeDueTransactions());

        verify(transactionService, never()).createTransactionForUser(any(), any());
        verify(recuringTransactionService, never()).save(any());
    }

    @Test
    void testCalculateNextDate_yearlyFrequency() {
        recurring.setFrequency(Frequency.YEARLY);
        when(recuringTransactionService.findAllActiveByNextExecutionDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(recurring));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(account.getId(), user.getId())).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.of(category));
        when(recuringTransactionService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.executeDueTransactions();

        assertEquals(LocalDate.now().plusYears(1), recurring.getNextExecutionDate());
    }
}

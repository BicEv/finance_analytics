package ru.bicev.finance_analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.bicev.finance_analytics.dto.RecurringTransactionDto;
import ru.bicev.finance_analytics.dto.RecurringTransactionRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;
import ru.bicev.finance_analytics.service.RecurringTransactionService;
import ru.bicev.finance_analytics.service.UserService;
import ru.bicev.finance_analytics.util.Frequency;

@ExtendWith(MockitoExtension.class)
public class RecuringTransactionServiceTest {

        @Mock
        private UserService userService;


        @Mock
        private CategoryRepository categoryRepository;

        @Mock
        private RecurringTransactionRepository recurringTransactionRepository;

        @InjectMocks
        private RecurringTransactionService service;

        private User user;
        private Category category;

        @BeforeEach
        void init() {
                user = User.builder().id(1L).email("test@mail.com").build();
                category = Category.builder().id(UUID.randomUUID()).user(user).name("Food").build();

                when(userService.getCurrentUser()).thenReturn(user);
        }

        // --------------------------------------------------------
        // createTransaction()
        // --------------------------------------------------------
        @Test
        void testCreateTransaction_success() {
                LocalDate next = LocalDate.now().plusDays(2);

                RecurringTransactionRequest request = new RecurringTransactionRequest(
                                category.getId(), // categoryId
                                new BigDecimal("9.99"), // amount
                                Frequency.MONTHLY, // frequency
                                next, // nextExecutionDate
                                "Netflix", // description
                                true // isActive
                );

                
                when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                                .thenReturn(Optional.of(category));
                when(recurringTransactionRepository.save(any()))
                                .thenAnswer(inv -> inv.getArgument(0));

                ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);

                RecurringTransactionDto result = service.createTransaction(request);

                verify(recurringTransactionRepository).save(captor.capture());
                RecurringTransaction saved = captor.getValue();

                assertEquals("Netflix", saved.getDescription());
                assertEquals(new BigDecimal("9.99").setScale(2), saved.getAmount());
                assertEquals(Frequency.MONTHLY, saved.getFrequency());
                assertTrue(saved.isActive());
                assertEquals(next, saved.getNextExecutionDate());
                assertNotNull(saved.getCreatedAt());
                assertEquals(category, saved.getCategory());
        }


        @Test
        void testCreateTransaction_categoryNotFound() {
                RecurringTransactionRequest request = new RecurringTransactionRequest(
                                UUID.randomUUID(),
                                BigDecimal.TEN,
                                Frequency.WEEKLY,
                                LocalDate.now().plusDays(1),
                                "Desc",
                                true);

                
                when(categoryRepository.findByIdAndUserId(request.categoryId(), user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.createTransaction(request));
        }

        @Test
        void testCreateTransaction_nextExecutionDateBeforeNow() {
                RecurringTransactionRequest request = new RecurringTransactionRequest(
                                category.getId(),
                                BigDecimal.TEN,
                                Frequency.WEEKLY,
                                LocalDate.now().minusDays(1),
                                "Desc",
                                true);

               
                when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                                .thenReturn(Optional.of(category));

                assertThrows(IllegalArgumentException.class,
                                () -> service.createTransaction(request));
        }

        // --------------------------------------------------------
        // getTransactionById()
        // --------------------------------------------------------
        @Test
        void testGetTransactionById_success() {
                UUID id = UUID.randomUUID();
                RecurringTransaction transaction = RecurringTransaction.builder()
                                .id(id)
                                .user(user)
                                .category(category)
                                .frequency(Frequency.MONTHLY)
                                .build();

                when(recurringTransactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(transaction));

                RecurringTransactionDto result = service.getTransactionById(id);

                assertEquals(id, result.id());
                assertEquals(transaction.getAmount(), result.amount());
        }

        @Test
        void testGetTransactionById_notFound() {
                UUID id = UUID.randomUUID();

                when(recurringTransactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.getTransactionById(id));
        }

        // --------------------------------------------------------
        // deleteTransaction()
        // --------------------------------------------------------
        @Test
        void testDeleteTransaction_success() {
                UUID id = UUID.randomUUID();
                RecurringTransaction transaction = RecurringTransaction.builder().id(id).user(user).build();

                when(recurringTransactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(transaction));

                service.deleteTransaction(id);

                verify(recurringTransactionRepository).delete(transaction);
        }

        @Test
        void testDeleteTransaction_notFound() {
                UUID id = UUID.randomUUID();
                when(recurringTransactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.deleteTransaction(id));
        }
}

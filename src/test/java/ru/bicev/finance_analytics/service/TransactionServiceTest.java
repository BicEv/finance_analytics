package ru.bicev.finance_analytics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

import ru.bicev.finance_analytics.dto.TransactionDto;
import ru.bicev.finance_analytics.dto.TransactionRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

        @Mock
        private UserService userService;

        @Mock
        private TransactionRepository transactionRepository;


        @Mock
        private CategoryRepository categoryRepository;

        @InjectMocks
        private TransactionService service;

        private User user;
        private Category category;

        @BeforeEach
        void init() {
                user = User.builder().id(1L).email("test@mail.com").build();
                category = Category.builder().id(UUID.randomUUID()).user(user).name("Food").build();

                lenient().when(userService.getCurrentUser()).thenReturn(user);
                lenient().when(userService.getCurrentUserId()).thenReturn(user.getId());
        }

        // --------------------------------------------------------
        // createTransaction()
        // --------------------------------------------------------
        @Test
        void testCreateTransaction_success() {
                LocalDate date = LocalDate.now();

                TransactionRequest request = new TransactionRequest(
                                category.getId(),
                                new BigDecimal("100.50"),
                                date,
                                "Groceries",
                                false);

                
                when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                                .thenReturn(Optional.of(category));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

                TransactionDto result = service.createTransaction(request);

                verify(transactionRepository).save(captor.capture());
                Transaction saved = captor.getValue();

                assertEquals(user, saved.getUser());
                assertEquals(category, saved.getCategory());
                assertEquals(new BigDecimal("100.50").setScale(2), saved.getAmount());
                assertEquals(date, saved.getDate());
                assertEquals("Groceries", saved.getDescription());
                assertFalse(saved.isPlanned());
                assertNotNull(saved.getCreatedAt());
        }


        @Test
        void testCreateTransaction_categoryNotFound() {
                TransactionRequest request = new TransactionRequest(
                                UUID.randomUUID(),
                                BigDecimal.TEN,
                                LocalDate.now(),
                                "Desc",
                                false);

                
                when(categoryRepository.findByIdAndUserId(request.categoryId(), user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.createTransaction(request));
        }

        // --------------------------------------------------------
        // getTransactionById()
        // --------------------------------------------------------
        @Test
        void testGetTransactionById_success() {
                UUID id = UUID.randomUUID();
                Transaction transaction = Transaction.builder().id(id).user(user).category(category).build();

                when(transactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(transaction));

                TransactionDto result = service.getTransactionById(id);

                assertEquals(id, result.id());
        }

        @Test
        void testGetTransactionById_notFound() {
                UUID id = UUID.randomUUID();

                when(transactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.getTransactionById(id));
        }

        // --------------------------------------------------------
        // updateTransaction()
        // --------------------------------------------------------
        @Test
        void testUpdateTransaction_success() {
                UUID id = UUID.randomUUID();
                Transaction existing = Transaction.builder()
                                .id(id)
                                .user(user)
                                .amount(new BigDecimal("50.00"))
                                .description("Old")
                                .isPlanned(false)
                                .build();

                TransactionRequest request = new TransactionRequest(
                                category.getId(),
                                new BigDecimal("200.00"),
                                LocalDate.now().plusDays(1),
                                "Updated",
                                true);

                when(transactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(existing));
                when(categoryRepository.findByIdAndUserId(category.getId(), user.getId()))
                                .thenReturn(Optional.of(category));
                when(transactionRepository.save(any(Transaction.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                TransactionDto updated = service.updateTransaction(id, request);

                assertEquals(new BigDecimal("200.00").setScale(2), updated.amount());
                assertEquals("Updated", updated.description());
                assertTrue(updated.isPlanned());
                assertEquals(category.getId(), updated.categoryId());
                assertEquals(request.date(), updated.date());
        }

        @Test
        void testUpdateTransaction_notFound() {
                UUID id = UUID.randomUUID();
                TransactionRequest request = new TransactionRequest(
                                category.getId(),
                                BigDecimal.ONE,
                                LocalDate.now(),
                                "Desc",
                                false);

                when(transactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.updateTransaction(id, request));
        }

        // --------------------------------------------------------
        // deleteTransaction()
        // --------------------------------------------------------
        @Test
        void testDeleteTransaction_success() {
                UUID id = UUID.randomUUID();
                Transaction transaction = Transaction.builder().id(id).user(user).build();

                when(transactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(transaction));

                service.deleteTransaction(id);

                verify(transactionRepository).delete(transaction);
        }

        @Test
        void testDeleteTransaction_notFound() {
                UUID id = UUID.randomUUID();

                when(transactionRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> service.deleteTransaction(id));
        }

        // --------------------------------------------------------
        // getTransactionsByDateBetween()
        // --------------------------------------------------------
        @Test
        void testGetTransactionsByDateBetween_success() {
                LocalDate from = LocalDate.now().minusDays(5);
                LocalDate to = LocalDate.now();

                Transaction t1 = Transaction.builder().id(UUID.randomUUID()).user(user).category(category).build();
                Transaction t2 = Transaction.builder().id(UUID.randomUUID()).user(user).category(category).build();

                when(transactionRepository.findAllByUserIdAndDateBetween(
                                user.getId(),  from, to))
                                .thenReturn(List.of(t1, t2));

                List<TransactionDto> result = service.getTransactionsByDateBetween(from, to);

                assertEquals(2, result.size());
                verify(transactionRepository).findAllByUserIdAndDateBetween(user.getId(), from,
                                to);
        }

        @Test
        void testGetTransactionsByDateBetween_invalidDates() {
                LocalDate from = LocalDate.now();
                LocalDate to = LocalDate.now().minusDays(1);

                assertThrows(IllegalArgumentException.class,
                                () -> service.getTransactionsByDateBetween(from, to));
        }
}

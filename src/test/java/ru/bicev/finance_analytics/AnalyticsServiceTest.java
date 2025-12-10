package ru.bicev.finance_analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.bicev.finance_analytics.dto.DateRange;
import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.RecurringTransaction;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.RecurringTransactionRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;
import ru.bicev.finance_analytics.service.AnalyticsService;
import ru.bicev.finance_analytics.service.UserService;
import ru.bicev.finance_analytics.util.CategoryType;
import ru.bicev.finance_analytics.util.Frequency;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

        @Mock
        private UserService userService;

        @Mock
        private RecurringTransactionRepository recurringTransactionRepository;

        @Mock
        private TransactionRepository transactionRepository;

        @Mock
        private BudgetRepository budgetRepository;

        @InjectMocks
        private AnalyticsService analyticsService;

        private Long userId;
        private UUID accountId;
        private UUID expCategoryId1;
        private UUID expCategoryId2;
        private UUID incCategoryId;
        private UUID budgetId;
        private YearMonth month;

        private User user;


        private Category catExpense1;
        private Category catExpense2;
        private Category catIncome;

        private Transaction tr1;
        private Transaction tr2;
        private Transaction tr3;
        private Transaction tr4;

        private RecurringTransaction rtr1;
        private RecurringTransaction rtr2;

        private Budget b1;

        @BeforeEach
        void setUp() {
                userId = 10L;
                accountId = UUID.randomUUID();
                expCategoryId1 = UUID.randomUUID();
                expCategoryId2 = UUID.randomUUID();
                incCategoryId = UUID.randomUUID();
                budgetId = UUID.randomUUID();
                month = YearMonth.of(2025, 10);

                user = User.builder()
                                .id(userId).email("test@email.com").name("John Doe").build();


                catExpense1 = Category.builder()
                                .id(expCategoryId1)
                                .name("Food")
                                .type(CategoryType.EXPENSE)
                                .user(user)
                                .build();

                catExpense2 = Category.builder()
                                .id(expCategoryId2)
                                .name("Entertainment")
                                .type(CategoryType.EXPENSE)
                                .user(user)
                                .build();

                catIncome = Category.builder()
                                .id(incCategoryId)
                                .name("Salary")
                                .type(CategoryType.INCOME)
                                .user(user)
                                .build();

                tr1 = Transaction.builder()
                                .id(UUID.randomUUID())
                                .category(catExpense1)
                                .date(LocalDate.of(2025, 10, 5))
                                .description("Bread")
                                .isPlanned(false)
                                .user(user)
                                .amount(BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP))
                                .build();

                tr2 = Transaction.builder()
                                .id(UUID.randomUUID())
                                .category(catExpense1)
                                .date(LocalDate.of(2025, 10, 11))
                                .description("Food supply")
                                .isPlanned(false)
                                .user(user)
                                .amount(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP))
                                .build();

                tr3 = Transaction.builder()
                                .id(UUID.randomUUID())
                                .category(catIncome)
                                .date(LocalDate.of(2025, 10, 15))
                                .description("Half of salary")
                                .isPlanned(false)
                                .user(user)
                                .amount(BigDecimal.valueOf(1500).setScale(2, RoundingMode.HALF_UP))
                                .build();

                tr4 = Transaction.builder()
                                .id(UUID.randomUUID())
                                .category(catExpense2)
                                .date(LocalDate.of(2025, 10, 22))
                                .description("Game copy")
                                .isPlanned(false)
                                .user(user)
                                .amount(BigDecimal.valueOf(60).setScale(2, RoundingMode.HALF_UP))
                                .build();

                rtr1 = RecurringTransaction.builder()
                                .id(UUID.randomUUID())
                                .category(catExpense2)
                                .frequency(Frequency.MONTHLY)
                                .nextExecutionDate(LocalDate.of(2025, 12, 25))
                                .description("Netflix")
                                .user(user)
                                .amount(BigDecimal.valueOf(10).setScale(2, RoundingMode.HALF_UP))
                                .isActive(true)
                                .build();

                rtr2 = RecurringTransaction.builder()
                                .id(UUID.randomUUID())
                                .category(catExpense2)
                                .nextExecutionDate(LocalDate.of(2025, 12, 31))
                                .description("Some subscription service")
                                .user(user)
                                .amount(BigDecimal.valueOf(7).setScale(2, RoundingMode.HALF_UP))
                                .isActive(true)
                                .build();

                b1 = Budget.builder()
                                .category(catExpense1)
                                .id(budgetId)
                                .limitAmount(BigDecimal.valueOf(250).setScale(2, RoundingMode.HALF_UP))
                                .user(user)
                                .month(month)
                                .build();

                when(userService.getCurrentUser()).thenReturn(user);
        }

        @Test
        void getExpensesByCategory() {
                when(transactionRepository.findAllByUserIdAndCategory_TypeAndDateBetween(userId, 
                                CategoryType.EXPENSE, month.atDay(1),
                                month.atEndOfMonth())).thenReturn(List.of(tr1, tr2, tr4));

                Map<String, BigDecimal> result = analyticsService.getExpensesByCategory(accountId, month);

                assertEquals(2, result.keySet().size());
                assertEquals(tr1.getAmount().add(tr2.getAmount()), result.get("Food"));
                assertEquals(tr4.getAmount(), result.get("Entertainment"));

        }

        @Test
        void testGetTopCategories() {
                when(transactionRepository.findAllByUserIdAndCategory_TypeAndDateBetween(userId, 
                                CategoryType.EXPENSE, month.atDay(1),
                                month.atEndOfMonth())).thenReturn(List.of(tr1, tr2, tr4));

                List<Map.Entry<String, BigDecimal>> result = analyticsService.getTopCategories(accountId, month, 3);

                assertNotNull(result);
                assertEquals(2, result.size());
                assertEquals("Food", result.get(0).getKey());
                assertEquals("Entertainment", result.get(1).getKey());
        }


        @Test
        void testGetDailyExpenses() {
                when(transactionRepository.findAllByUserIdAndCategory_TypeAndDateBetween(userId, 
                                CategoryType.EXPENSE, month.atDay(1),
                                month.atEndOfMonth())).thenReturn(List.of(tr1, tr2, tr4));

                Map<LocalDate, BigDecimal> result = analyticsService.getDailyExpenses(accountId, month);

                assertNotNull(result);
                assertEquals(tr1.getAmount(), result.get(tr1.getDate()));
                assertEquals(tr2.getAmount(), result.get(tr2.getDate()));
                assertEquals(tr4.getAmount(), result.get(tr4.getDate()));
        }

        @Test
        void testGetMonthlyExpenses() {
                when(transactionRepository.findAllByUserIdAndCategory_TypeAndDateBetween(userId, 
                                CategoryType.EXPENSE, month.atDay(1),
                                month.atEndOfMonth())).thenReturn(List.of(tr1, tr2, tr4));

                Map<String, BigDecimal> result = analyticsService.getMonthlyExpenses(accountId,
                                DateRange.ofMonth(month));

                assertNotNull(result);
                assertEquals(tr1.getAmount().add(tr2.getAmount()).add(tr4.getAmount()), result.get("10.2025"));
        }

        @Test
        void testGetSummary() {
                when(transactionRepository.findAllByUserIdAndDateBetween(userId, month.atDay(1),
                                month.atEndOfMonth())).thenReturn(List.of(tr1, tr2, tr3, tr4));

                Map<String, BigDecimal> result = analyticsService.getSummary(month);

                assertNotNull(result);
                assertEquals(tr3.getAmount(), result.get("income"));
                assertEquals(tr1.getAmount().add(tr2.getAmount()).add(tr4.getAmount()), result.get("expense"));
                assertEquals(tr3.getAmount().subtract(tr1.getAmount().add(tr2.getAmount()).add(tr4.getAmount())),
                                result.get("balance"));

        }

        @Test
        void testGetCategoryBudgetStatus() {
                lenient().when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(b1));
                when(transactionRepository.findAllByUserIdAndCategoryIdAndDateBetween(userId, catExpense1.getId(),
                                month.atDay(1),
                                month.atEndOfMonth())).thenReturn(List.of(tr1, tr2));

                Map<String, Object> result = analyticsService.getCategoryBudgetStatus(budgetId);

                assertNotNull(result);
                assertEquals(b1.getCategory().getName(), result.get("category"));
                assertEquals(b1.getLimitAmount(), result.get("limit"));
                assertEquals(tr1.getAmount().add(tr2.getAmount()), result.get("spent"));
                assertEquals(
                                tr1.getAmount().add(tr2.getAmount())
                                                .multiply(BigDecimal.valueOf(100).divide(b1.getLimitAmount()))
                                                .setScale(2, RoundingMode.HALF_UP),
                                result.get("percentUsed"));

        }

        @Test
        void testGetCategoryBudgetStatus_budgetNotFound() {
                when(budgetRepository.findByIdAndUserId(budgetId, userId))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> analyticsService.getCategoryBudgetStatus(budgetId));
        }

        @Test
        void testGetUpcomingRecurringPayments() {
                when(recurringTransactionRepository.findAllByUserIdAndActiveAndNextExecutionDateGreaterThan(userId,
                                true,
                                LocalDate.now())).thenReturn(List.of(rtr1, rtr2));

                Map<String, BigDecimal> result = analyticsService.getUpcomingRecurringPayments();

                assertNotNull(result);
                assertEquals(rtr1.getAmount().add(rtr2.getAmount()), result.get("12.2025"));
        }

}

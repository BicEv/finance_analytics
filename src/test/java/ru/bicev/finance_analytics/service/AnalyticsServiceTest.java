package ru.bicev.finance_analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.bicev.finance_analytics.dto.CategoryBudgetStatusDto;
import ru.bicev.finance_analytics.dto.CategoryExpenseDto;
import ru.bicev.finance_analytics.dto.DailyExpenseDto;
import ru.bicev.finance_analytics.dto.DateRange;
import ru.bicev.finance_analytics.dto.MonthlyExpenseDto;
import ru.bicev.finance_analytics.dto.RecurringForecastDto;
import ru.bicev.finance_analytics.dto.SummaryDto;
import ru.bicev.finance_analytics.dto.TopCategoryDto;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.JooqAnalyticsRepository;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

        @Mock
        private UserService userService;

        @Mock
        private JooqAnalyticsRepository jooqRepo;

        @InjectMocks
        private AnalyticsService analyticsService;

        private Long userId;
        private UUID budgetId;
        private YearMonth month;

        private User user;

        private List<CategoryExpenseDto> categoryExpenseDtos;
        private List<TopCategoryDto> topCategoryDtos;
        private List<DailyExpenseDto> dailyExpenseDtos;
        private List<MonthlyExpenseDto> monthlyExpenseDtos;
        private SummaryDto summaryDto;
        private CategoryBudgetStatusDto categoryBudgetStatusDto;
        private List<RecurringForecastDto> recurringForecastDtos;

        @BeforeEach
        void setUp() {
                userId = 10L;

                budgetId = UUID.randomUUID();

                month = YearMonth.of(2025, 12);

                categoryExpenseDtos = List.of(
                                new CategoryExpenseDto("Food", BigDecimal.valueOf(12500)),
                                new CategoryExpenseDto("Subscriptions", BigDecimal.valueOf(1500)));

                topCategoryDtos = List.of(
                                new TopCategoryDto("Food", BigDecimal.valueOf(12500)),
                                new TopCategoryDto("Clothes", BigDecimal.valueOf(9000)));

                dailyExpenseDtos = List.of(
                                new DailyExpenseDto(LocalDate.of(2025, 12, 10), BigDecimal.valueOf(2200)),
                                new DailyExpenseDto(LocalDate.of(2025, 12, 12), BigDecimal.valueOf(3300)),
                                new DailyExpenseDto(LocalDate.of(2025, 12, 21), BigDecimal.valueOf(1700)));

                monthlyExpenseDtos = List.of(
                                new MonthlyExpenseDto("10-2025", BigDecimal.valueOf(25000)),
                                new MonthlyExpenseDto("11-2025", BigDecimal.valueOf(25000)),
                                new MonthlyExpenseDto("12-2025", BigDecimal.valueOf(25000)));

                summaryDto = new SummaryDto(BigDecimal.valueOf(20000), BigDecimal.valueOf(17000),
                                BigDecimal.valueOf(3000));

                categoryBudgetStatusDto = new CategoryBudgetStatusDto(
                                "Food",
                                BigDecimal.valueOf(10000).setScale(2),
                                BigDecimal.valueOf(7500).setScale(2),
                                BigDecimal.valueOf(75).setScale(2));

                recurringForecastDtos = List.of(
                                new RecurringForecastDto("06-2030", BigDecimal.valueOf(1000)),
                                new RecurringForecastDto("07-2030", BigDecimal.valueOf(1200)));

                user = User.builder()
                                .id(userId).email("test@email.com").name("John Doe").build();

                lenient().when(userService.getCurrentUser()).thenReturn(user);
                lenient().when(userService.getCurrentUserId()).thenReturn(10L);
        }

        @Test
        void getExpensesByCategory() {
                when(jooqRepo.getExpensesByCategory(userId, month.atDay(1), month.atEndOfMonth()))
                                .thenReturn(categoryExpenseDtos);

                var result = analyticsService.getExpensesByCategory(month);

                assertEquals(categoryExpenseDtos.size(), result.size());
                assertEquals(categoryExpenseDtos.get(0).category(), result.get(0).category());
                assertEquals(categoryExpenseDtos.get(1).category(), result.get(1).category());
                assertEquals(categoryExpenseDtos.get(0).total(), result.get(0).total());
                assertEquals(categoryExpenseDtos.get(1).total(), result.get(1).total());

        }

        @Test
        void testGetTopCategories() {
                when(jooqRepo.getTopCategories(userId, month.atDay(1), month.atEndOfMonth(), 2))
                                .thenReturn(topCategoryDtos);

                var result = analyticsService.getTopCategories(month, 2);

                assertNotNull(result);
                assertEquals(2, result.size());
                assertEquals(topCategoryDtos.get(0).category(), result.get(0).category());
                assertEquals(topCategoryDtos.get(1).category(), result.get(1).category());
                assertEquals(topCategoryDtos.get(0).total(), result.get(0).total());
                assertEquals(topCategoryDtos.get(1).total(), result.get(1).total());
        }

        @Test
        void testGetDailyExpenses() {
                when(jooqRepo.getDailyExpenses(userId, month.atDay(1), month.atEndOfMonth()))
                                .thenReturn(dailyExpenseDtos);

                var result = analyticsService.getDailyExpenses(month);

                assertNotNull(result);
                assertEquals(dailyExpenseDtos.size(), result.size());
                assertEquals(dailyExpenseDtos.get(0).date(), result.get(0).date());
                assertEquals(dailyExpenseDtos.get(1).date(), result.get(1).date());
                assertEquals(dailyExpenseDtos.get(2).date(), result.get(2).date());
                assertEquals(dailyExpenseDtos.get(0).amount(), result.get(0).amount());
                assertEquals(dailyExpenseDtos.get(1).amount(), result.get(1).amount());
                assertEquals(dailyExpenseDtos.get(2).amount(), result.get(2).amount());

        }

        @Test
        void testGetMonthlyExpenses() {
                LocalDate start = LocalDate.of(2025, 10, 1);
                LocalDate end = LocalDate.of(2025, 12, 31);
                when(jooqRepo.getMonthlyExpenses(userId, start, end))
                                .thenReturn(monthlyExpenseDtos);

                var result = analyticsService.getMonthlyExpenses(
                                new DateRange(start, end));

                assertNotNull(result);
                assertEquals(monthlyExpenseDtos.size(), result.size());
                assertEquals(monthlyExpenseDtos.get(0).month(), result.get(0).month());
                assertEquals(monthlyExpenseDtos.get(1).month(), result.get(1).month());
                assertEquals(monthlyExpenseDtos.get(2).month(), result.get(2).month());
                assertEquals(monthlyExpenseDtos.get(0).total(), result.get(0).total());
                assertEquals(monthlyExpenseDtos.get(1).total(), result.get(1).total());
                assertEquals(monthlyExpenseDtos.get(2).total(), result.get(2).total());
        }

        @Test
        void testGetMonthlyExpenses_InvalidRange() {
                assertThrows(IllegalStateException.class, () -> analyticsService.getMonthlyExpenses(
                                new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2025, 12, 31))));
        }

        @Test
        void testGetSummary() {
                when(jooqRepo.getSummaryForMonth(userId, month.atDay(1), month.atDay(31))).thenReturn(summaryDto);

                var result = analyticsService.getSummary(month);

                assertNotNull(result);
                assertEquals(summaryDto.income(), result.income());
                assertEquals(summaryDto.expense(), result.expense());
                assertEquals(summaryDto.balance(), result.balance());

        }

        @Test
        void testGetCategoryBudgetStatus() {
                when(jooqRepo.getCategoryBudgetStatus(userId, budgetId))
                                .thenReturn(Optional.of(categoryBudgetStatusDto));

                var result = analyticsService.getCategoryBudgetStatus(budgetId);

                assertNotNull(result);
                assertEquals(categoryBudgetStatusDto.category(), result.category());
                assertEquals(categoryBudgetStatusDto.limit(), result.limit());
                assertEquals(categoryBudgetStatusDto.percentUsed(), result.percentUsed());

        }

        @Test
        void testGetCategoryBudgetStatus_budgetNotFound() {
                when(jooqRepo.getCategoryBudgetStatus(userId, budgetId))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> analyticsService.getCategoryBudgetStatus(budgetId));
        }

        @Test
        void testGetUpcomingRecurringPayments() {
                when(jooqRepo.getUpcomingPayments(userId)).thenReturn(recurringForecastDtos);

                var result = analyticsService.getUpcomingRecurringPayments();

                assertNotNull(result);
                assertEquals(recurringForecastDtos.get(0).month(), result.get(0).month());
                assertEquals(recurringForecastDtos.get(1).month(), result.get(1).month());
                assertEquals(recurringForecastDtos.get(0).expectedAmount(), result.get(0).expectedAmount());
                assertEquals(recurringForecastDtos.get(1).expectedAmount(), result.get(1).expectedAmount());
        }

}

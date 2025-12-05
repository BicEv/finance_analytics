package ru.bicev.finance_analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
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

import ru.bicev.finance_analytics.dto.BudgetRequest;
import ru.bicev.finance_analytics.entity.Budget;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.service.BudgetService;
import ru.bicev.finance_analytics.service.UserService;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private Category category;
    private BigDecimal amount;

    @BeforeEach
    void init() {
        user = User.builder()
                .id(100L)
                .email("test@mail.com")
                .build();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Food")
                .user(user)
                .build();

        when(userService.getCurrentUser()).thenReturn(user);
    }

    // --------------------------------------------------------
    // createBudget()
    // --------------------------------------------------------
    @Test
    void testCreateBudget_success() {
        UUID categoryId = category.getId();
        YearMonth month = YearMonth.of(2024, 10);
        amount = BigDecimal.valueOf(500).setScale(2,RoundingMode.HALF_UP);

        BudgetRequest request = new BudgetRequest(categoryId, month, amount);

        when(categoryRepository.findByIdAndUserId(categoryId, user.getId()))
                .thenReturn(Optional.of(category));

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);

        Budget result = budgetService.createBudget(request);

        verify(budgetRepository).save(captor.capture());
        Budget saved = captor.getValue();

        assertEquals(category, saved.getCategory());
        assertEquals(month, saved.getMonth());
        assertEquals(amount, saved.getLimitAmount());
        assertEquals(user, saved.getUser());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void testCreateBudget_categoryNotFound() {
        UUID id = UUID.randomUUID();
        amount = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        BudgetRequest request = new BudgetRequest(id, YearMonth.now(), amount);

        when(categoryRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> budgetService.createBudget(request));
    }

    // --------------------------------------------------------
    // getBudgetById()
    // --------------------------------------------------------
    @Test
    void testGetBudgetById_success() {
        UUID id = UUID.randomUUID();
        amount = BigDecimal.valueOf(300.0);

        Budget budget = Budget.builder()
                .id(id)
                .user(user)
                .limitAmount(amount)
                .build();

        when(budgetRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(budget));

        Budget result = budgetService.getBudgetById(id);

        assertEquals(id, result.getId());
        assertEquals(amount, result.getLimitAmount());
    }

    @Test
    void testGetBudgetById_notFound() {
        UUID id = UUID.randomUUID();

        when(budgetRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> budgetService.getBudgetById(id));
    }

    // --------------------------------------------------------
    // getAllBudgetsForUser()
    // --------------------------------------------------------
    @Test
    void testGetAllBudgetsForUser_success() {
        amount = BigDecimal.valueOf(150).setScale(2, RoundingMode.HALF_UP);
        Budget b1 = Budget.builder().id(UUID.randomUUID()).user(user).limitAmount(amount).build();
        Budget b2 = Budget.builder().id(UUID.randomUUID()).user(user).limitAmount(amount).build();

        when(budgetRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of(b1, b2));

        List<Budget> result = budgetService.getAllBudgetsForUser();

        assertEquals(2, result.size());
        verify(budgetRepository).findAllByUserId(user.getId());
    }

    // --------------------------------------------------------
    // getBudgetsForMonth()
    // --------------------------------------------------------
    @Test
    void testGetBudgetsForMonth_success() {
        YearMonth month = YearMonth.of(2024, 11);

        Budget b = Budget.builder().id(UUID.randomUUID()).month(month).user(user).build();

        when(budgetRepository.findByUserIdAndMonth(user.getId(), month))
                .thenReturn(List.of(b));

        List<Budget> result = budgetService.getBudgetsForMonth(month);

        assertEquals(1, result.size());
        assertEquals(month, result.get(0).getMonth());

        verify(budgetRepository).findByUserIdAndMonth(user.getId(), month);
    }

    // --------------------------------------------------------
    // updateBudget()
    // --------------------------------------------------------
    @Test
    void testUpdateBudget_success() {
        UUID id = UUID.randomUUID();
        YearMonth newMonth = YearMonth.of(2025, 1);
        amount = BigDecimal.valueOf(999).setScale(2,RoundingMode.HALF_UP);

        Budget existing = Budget.builder()
                .id(id)
                .user(user)
                .month(YearMonth.of(2024, 10))
                .limitAmount(BigDecimal.valueOf(200.0))
                .build();

        BudgetRequest request = new BudgetRequest(
                category.getId(),
                newMonth,
                amount);

        when(budgetRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(existing));

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Budget result = budgetService.updateBudget(id, request);

        assertEquals(newMonth, result.getMonth());
        assertEquals(amount, result.getLimitAmount());

        verify(budgetRepository).save(existing);
    }

    @Test
    void testUpdateBudget_notFound() {
        UUID id = UUID.randomUUID();
        amount = BigDecimal.valueOf(123).setScale(2, RoundingMode.HALF_UP);
        BudgetRequest request = new BudgetRequest(UUID.randomUUID(), YearMonth.now(), amount);

        when(budgetRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> budgetService.updateBudget(id, request));
    }

    // --------------------------------------------------------
    // deleteBudget()
    // --------------------------------------------------------
    @Test
    void testDeleteBudget_success() {
        UUID id = UUID.randomUUID();

        Budget b = Budget.builder().id(id).user(user).build();

        when(budgetRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(b));

        budgetService.deleteBudget(id);

        verify(budgetRepository).delete(b);
    }

    @Test
    void testDeleteBudget_notFound() {
        UUID id = UUID.randomUUID();

        when(budgetRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> budgetService.deleteBudget(id));
    }
}

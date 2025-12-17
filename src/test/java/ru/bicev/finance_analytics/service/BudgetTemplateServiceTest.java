package ru.bicev.finance_analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.bicev.finance_analytics.dto.BudgetTemplateRequest;
import ru.bicev.finance_analytics.dto.BudgetTemplateUpdateRequest;
import ru.bicev.finance_analytics.entity.BudgetTemplate;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.BudgetTemplateRepository;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.util.CategoryType;

@ExtendWith(MockitoExtension.class)
public class BudgetTemplateServiceTest {

    @Mock
    private BudgetTemplateRepository repository;

    @Mock
    private UserService userService;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetTemplateService budgetTemplateService;

    private User user;
    private Category category;
    private BudgetTemplate template1;
    private BudgetTemplate template2;

    private BudgetTemplateRequest request;
    private BudgetTemplateUpdateRequest updateRequest;

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.yyyy");

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(100L)
                .email("test@email.com")
                .name("John Doe")
                .build();
        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Food")
                .user(user)
                .type(CategoryType.EXPENSE)
                .build();
        template1 = BudgetTemplate.builder()
                .id(UUID.randomUUID())
                .active(true)
                .amount(BigDecimal.valueOf(100.00))
                .category(category)
                .startMonth(YearMonth.of(2025, 1))
                .user(user)
                .build();
        template2 = BudgetTemplate.builder()
                .id(UUID.randomUUID())
                .active(true)
                .amount(BigDecimal.valueOf(200.00))
                .category(category)
                .startMonth(YearMonth.of(2025, 5))
                .user(user)
                .build();
        request = new BudgetTemplateRequest(category.getId(), BigDecimal.valueOf(100.00), true, YearMonth.of(2025, 1));
        updateRequest = new BudgetTemplateUpdateRequest(null, BigDecimal.valueOf(200.00), true, YearMonth.of(2025, 5));

    }

    // ----------------------------------
    // createBudgetTemplate()
    // ----------------------------------
    @Test
    void testCreateBudgetTemplate_success() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.of(category));
        when(repository.existsByUserIdAndCategoryId(user.getId(), category.getId())).thenReturn(false);
        when(repository.save(any(BudgetTemplate.class))).thenReturn(template1);

        var result = budgetTemplateService.createBudgetTemplate(request);

        assertEquals(request.active(), result.active());
        assertEquals(request.amount(), result.amount());
        assertEquals(request.startMonth().format(FORMAT), result.startMonth());

        verify(repository, times(1)).save(any(BudgetTemplate.class));
    }

    @Test
    void testCreateBudgetTemplate_categoryNotFound() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> budgetTemplateService.createBudgetTemplate(request));
    }

    @Test
    void testCreateBudgetTemplate_templateExists() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(categoryRepository.findByIdAndUserId(category.getId(), user.getId())).thenReturn(Optional.of(category));
        when(repository.existsByUserIdAndCategoryId(user.getId(), category.getId())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> budgetTemplateService.createBudgetTemplate(request));
    }

    // ----------------------------------
    // deleteBudgetTemplate()
    // ----------------------------------
    @Test
    void testDeleteBudgetTemplate_success() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.of(template1));

        budgetTemplateService.deleteBudgetTemplate(template1.getId());
        ;

        verify(repository, times(1)).delete(template1);
    }

    @Test
    void testDeleteBudgetTemplate_templateNotFound() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> budgetTemplateService.deleteBudgetTemplate(template1.getId()));
    }

    // ----------------------------------
    // findAllBudgetTemplatesForCurrentUser()
    // ----------------------------------
    @Test
    void testFindAllBudgetTemplatesForCurrentUser_success() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findAllByUserId(user.getId())).thenReturn(List.of(template1, template2));

        var result = budgetTemplateService.findAllBudgetTemplatesForCurrentUser();

        assertEquals(2, result.size());
        assertEquals(template1.getAmount(), result.get(0).amount());
        assertEquals(template2.getAmount(), result.get(1).amount());
    }

    @Test
    void testFindAllBudgetTemplatesForCurrentUser_noneFound() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findAllByUserId(user.getId())).thenReturn(List.of());

        var result = budgetTemplateService.findAllBudgetTemplatesForCurrentUser();

        assertEquals(0, result.size());
    }

    // ----------------------------------
    // getBudgetTemplateById()
    // ----------------------------------
    @Test
    void testGetBudgetTemplateById_success() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.of(template1));

        var result = budgetTemplateService.getBudgetTemplateById(template1.getId());

        assertEquals(template1.getAmount(), result.amount());
        assertEquals(template1.getId(), result.id());
        assertEquals(template1.getStartMonth().format(FORMAT), result.startMonth());

    }

    @Test
    void testGetBudgetTemplateById_templateNotFound() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> budgetTemplateService.deleteBudgetTemplate(template1.getId()));
    }

    // ----------------------------------
    // updateBudgetTemplate()
    // ----------------------------------
    @Test
    void testUpdateBudgetTemplate_success() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.of(template1));
        when(repository.save(any(BudgetTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = budgetTemplateService.updateBudgetTemplate(template1.getId(), updateRequest);

        assertEquals(updateRequest.amount(), result.amount());
        assertEquals(updateRequest.startMonth().format(FORMAT), result.startMonth());
        assertEquals(updateRequest.active(), result.active());
    }

    @Test
    void testUpdateBudgetTemplate_templateNotFound() {
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> budgetTemplateService.updateBudgetTemplate(template1.getId(), updateRequest));
    }

    @Test
    void testUpdateBudgetTemplate_categoryNotFound() {
        UUID categoryId = UUID.randomUUID();
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.findByIdAndUserId(template1.getId(), user.getId())).thenReturn(Optional.of(template1));
        when(categoryRepository.findByIdAndUserId(categoryId, user.getId())).thenReturn(Optional.empty());

        var invalidUpdateRequest = new BudgetTemplateUpdateRequest(categoryId, null, null, null);

        assertThrows(NotFoundException.class,
                () -> budgetTemplateService.updateBudgetTemplate(template1.getId(), invalidUpdateRequest));
    }
}

package ru.bicev.finance_analytics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.bicev.finance_analytics.dto.CategoryDto;
import ru.bicev.finance_analytics.dto.CategoryRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.util.CategoryType;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

        @Mock
        private UserService userService;

        @Mock
        private CategoryRepository categoryRepository;

        @InjectMocks
        private CategoryService categoryService;

        private User user;

        @BeforeEach
        void init() {
                user = User.builder()
                                .id(10L)
                                .email("test@mail.com")
                                .build();

                when(userService.getCurrentUser()).thenReturn(user);
        }

        // --------------------------------------------------------
        // createCategory()
        // --------------------------------------------------------
        @Test
        void testCreateCategory_success() {
                CategoryRequest request = new CategoryRequest(
                                "Food",
                                CategoryType.EXPENSE,
                                "#ff0000");

                when(categoryRepository.save(any(Category.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

                CategoryDto result = categoryService.createCategory(request);

                verify(categoryRepository).save(captor.capture());
                Category saved = captor.getValue();

                assertEquals("Food", saved.getName());
                assertEquals(CategoryType.EXPENSE, saved.getType());
                assertEquals("#ff0000", saved.getColor());
                assertEquals(user, saved.getUser());
                assertNotNull(saved.getCreatedAt());
        }

        // --------------------------------------------------------
        // getUserCategories()
        // --------------------------------------------------------
        @Test
        void testGetUserCategories_noAccountId() {
                List<Category> categories = List.of(
                                Category.builder().id(UUID.randomUUID()).user(user).name("Food")
                                                .type(CategoryType.EXPENSE)
                                                .build());

                when(categoryRepository.findAllByUserId(user.getId()))
                                .thenReturn(categories);

                List<CategoryDto> result = categoryService.getUserCategories();

                assertEquals(1, result.size());
                verify(categoryRepository).findAllByUserId(user.getId());
        }

        @Test
        void testGetUserCategories_withAccountId() {
                UUID accountId = UUID.randomUUID();
                List<Category> categories = List.of(
                                Category.builder().id(UUID.randomUUID()).user(user).name("Transport")
                                                .type(CategoryType.EXPENSE)
                                                .build());

                when(categoryRepository.findAllByUserId(user.getId()))
                                .thenReturn(categories);

                List<CategoryDto> result = categoryService.getUserCategories();

                assertEquals(1, result.size());
                verify(categoryRepository).findAllByUserId(user.getId());
        }

        // --------------------------------------------------------
        // getCategoryById()
        // --------------------------------------------------------
        @Test
        void testGetCategoryById_success() {
                UUID categoryId = UUID.randomUUID();
                Category cat = Category.builder()
                                .id(categoryId)
                                .user(user)
                                .name("Bills")
                                .type(CategoryType.EXPENSE)
                                .build();

                when(categoryRepository.findByIdAndUserId(categoryId, user.getId()))
                                .thenReturn(Optional.of(cat));

                CategoryDto result = categoryService.getCategoryById(categoryId);

                assertEquals(categoryId, result.id());
                assertEquals("Bills", result.name());
        }

        @Test
        void testGetCategoryById_notFound() {
                UUID categoryId = UUID.randomUUID();

                when(categoryRepository.findByIdAndUserId(categoryId, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> categoryService.getCategoryById(categoryId));
        }

        // --------------------------------------------------------
        // updateCategory()
        // --------------------------------------------------------
        @Test
        void testUpdateCategory_success() {
                UUID id = UUID.randomUUID();

                Category existing = Category.builder()
                                .id(id)
                                .name("Old")
                                .color("#000000")
                                .user(user)
                                .type(CategoryType.EXPENSE)
                                .build();

                CategoryRequest request = new CategoryRequest(
                                "Food",
                                CategoryType.EXPENSE,
                                "#ffffff");

                when(categoryRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(existing));

                when(categoryRepository.save(any(Category.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                CategoryDto updated = categoryService.updateCategory(id, request);

                assertEquals("Food", updated.name());
                assertEquals("#ffffff", updated.color());

                verify(categoryRepository).save(existing);
        }

        @Test
        void testUpdateCategory_notFound() {
                UUID id = UUID.randomUUID();

                CategoryRequest request = new CategoryRequest(
                                "Food",
                                CategoryType.EXPENSE,
                                "#ff0000");

                when(categoryRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> categoryService.updateCategory(id, request));
        }

        // --------------------------------------------------------
        // deleteCategory()
        // --------------------------------------------------------
        @Test
        void testDeleteCategory_success() {
                UUID id = UUID.randomUUID();
                Category cat = Category.builder()
                                .id(id)
                                .user(user)
                                .build();

                when(categoryRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.of(cat));

                categoryService.deleteCategory(id);

                verify(categoryRepository).delete(cat);
        }

        @Test
        void testDeleteCategory_notFound() {
                UUID id = UUID.randomUUID();

                when(categoryRepository.findByIdAndUserId(id, user.getId()))
                                .thenReturn(Optional.empty());

                assertThrows(NotFoundException.class,
                                () -> categoryService.deleteCategory(id));
        }

        // --------------------------------------------------------
        // getCategoriesByType()
        // --------------------------------------------------------
        @Test
        void testGetCategoriesByType_success() {
                List<Category> categories = List.of(
                                Category.builder().id(UUID.randomUUID()).user(user).name("Salary")
                                                .type(CategoryType.INCOME).build());

                when(categoryRepository.findAllByUserIdAndType(user.getId(), CategoryType.INCOME))
                                .thenReturn(categories);

                List<CategoryDto> result = categoryService.getCategoriesByType(CategoryType.INCOME);

                assertEquals(1, result.size());
                verify(categoryRepository).findAllByUserIdAndType(user.getId(), CategoryType.INCOME);
        }

        // --------------------------------------------------------
        // getAllCategoriesForUser()
        // --------------------------------------------------------
        @Test
        void testGetAllCategoriesForUser_success() {
                List<Category> categories = List.of(
                                Category.builder().id(UUID.randomUUID()).user(user).name("Salary")
                                                .type(CategoryType.INCOME).build(),
                                Category.builder().id(UUID.randomUUID()).user(user).name("Food")
                                                .type(CategoryType.EXPENSE).build());

                when(categoryRepository.findAllByUserId(user.getId())).thenReturn(categories);

                List<CategoryDto> result = categoryService.getAllCategoriesForUser();

                assertEquals(2, result.size());
                assertEquals("Salary", result.get(0).name());
                assertEquals("Food", result.get(1).name());
        }

}

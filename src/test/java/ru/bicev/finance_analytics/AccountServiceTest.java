package ru.bicev.finance_analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import ru.bicev.finance_analytics.dto.CreateAccountRequest;
import ru.bicev.finance_analytics.entity.Account;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.AccountRepository;
import ru.bicev.finance_analytics.service.AccountService;
import ru.bicev.finance_analytics.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(100L)
                .email("test@email.com")
                .build();

        when(userService.getCurrentUser()).thenReturn(user);
    }

    // --------------------------------------------------------
    // getUserAccounts()
    // --------------------------------------------------------
    @Test
    void testGetUserAccounts_success() {
        Account acc1 = Account.builder().id(UUID.randomUUID()).user(user).name("Account#1").build();
        Account acc2 = Account.builder().id(UUID.randomUUID()).user(user).name("Account#2").build();

        when(accountRepository.findAllByUserId(user.getId())).thenReturn(List.of(acc1, acc2));

        List<Account> result = accountService.getUserAccounts();

        assertEquals(2, result.size());
        assertEquals(acc1.getName(), result.get(0).getName());
        assertEquals(acc2.getName(), result.get(1).getName());

        verify(accountRepository).findAllByUserId(user.getId());
    }

    // --------------------------------------------------------
    // createAccount()
    // --------------------------------------------------------
    @Test
    void testCreateAccount_success() {
        CreateAccountRequest request = new CreateAccountRequest("Main", "USD");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.createAccount(request);

        verify(accountRepository).save(captor.capture());

        Account saved = captor.getValue();

        assertEquals("Main", saved.getName());
        assertEquals("USD", saved.getCurrency());
        assertEquals(user, saved.getUser());
        assertNotNull(saved.getCreatedAt());
    }

    // --------------------------------------------------------
    // getAccountById()
    // --------------------------------------------------------
    @Test
    void testGetAccountById_success() {
        UUID id = UUID.randomUUID();

        Account acc = Account.builder()
                .id(id)
                .name("Wallet")
                .user(user)
                .build();

        when(accountRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(acc));

        Account result = accountService.getAccountById(id);

        assertEquals(id, result.getId());
        assertEquals("Wallet", result.getName());
    }

    @Test
    void testGetAccountById_notFound() {
        UUID id = UUID.randomUUID();

        when(accountRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.getAccountById(id));
    }

    // --------------------------------------------------------
    // updateAccount()
    // --------------------------------------------------------
    @Test
    void testUpdateAccount_success() {
        UUID id = UUID.randomUUID();

        Account acc = Account.builder()
                .id(id)
                .name("Old")
                .user(user)
                .build();

        when(accountRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(acc));

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.updateAccount(id, "NewName");

        assertEquals("NewName", result.getName());
        verify(accountRepository).save(acc);
    }

    @Test
    void testUpdateAccount_notFound() {
        UUID id = UUID.randomUUID();

        when(accountRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.updateAccount(id, "NewName"));
    }


    // --------------------------------------------------------
    // deleteAccount()
    // --------------------------------------------------------
    @Test
    void testDeleteAccount_success() {
        UUID id = UUID.randomUUID();

        Account acc = Account.builder()
                .id(id)
                .name("ToDelete")
                .user(user)
                .build();

        when(accountRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(acc));

        accountService.deleteAccount(id);

        verify(accountRepository).delete(acc);
    }

    @Test
    void testDeleteAccount_notFound() {
        UUID id = UUID.randomUUID();

        when(accountRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> accountService.deleteAccount(id));
    }
}

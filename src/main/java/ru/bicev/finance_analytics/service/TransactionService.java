package ru.bicev.finance_analytics.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.bicev.finance_analytics.dto.TransactionDto;
import ru.bicev.finance_analytics.dto.TransactionRequest;
import ru.bicev.finance_analytics.entity.Category;
import ru.bicev.finance_analytics.entity.Transaction;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.exception.NotFoundException;
import ru.bicev.finance_analytics.repo.CategoryRepository;
import ru.bicev.finance_analytics.repo.TransactionRepository;

/**
 * Сервис для управления транзакциями пользователя
 */
@Service
public class TransactionService {

    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(UserService userService, TransactionRepository transactionRepository,
            CategoryRepository categoryRepository) {
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Создает новую транзкацию для текущего пользователя
     * @param request запрос, содержащий данные новой транзакции
     * @return дто, содержащее данные созданной транзакции
     * @throws NotFoundException если категория укзанная в запросе не существует
     */
    @Transactional
    public TransactionDto createTransaction(TransactionRequest request) {
        User user = getCurrentUser();
        Category category = getCategory(request.categoryId(), user.getId());

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .date(request.date() != null ? request.date() : LocalDate.now())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .createdAt(LocalDateTime.now())
                .description(request.description())
                .isPlanned(request.isPlanned())
                .build();
        logger.debug("createTransaction() for user: {}", user.getId());
        return toDto(transactionRepository.save(transaction));
    }

    /**
     * Служебный метод для создания новой транзакции для укзанного пользователя
     * @param user пользователь, для которого создается транзакция
     * @param request запрос, содержащий данные для создания транзакции
     * @return созданная транзакция
     * @throws NotFoundException если категория укзанная в запросе не существует
     */
    @Transactional
    public Transaction createTransactionForUser(User user, TransactionRequest request) {
        Category category = getCategory(request.categoryId(), user.getId());

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .date(request.date() != null ? request.date() : LocalDate.now())
                .amount(request.amount().setScale(2, RoundingMode.HALF_UP))
                .createdAt(LocalDateTime.now())
                .description(request.description())
                .isPlanned(request.isPlanned())
                .build();
        logger.debug("createTransactionForUser() for user: {}", user.getId());
        return transactionRepository.save(transaction);
    }

    /**
     * Возвращает все транзакции текущего пользователя
     * @return список всех транзакций текущего пользователя
     */
    public List<TransactionDto> getTransactions() {
        logger.debug("getTransactions()");
        return transactionRepository.findAllByUserId(getCurrentUserId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает все транзакции текущего пользователя за указанный период
     * @param from дата начала периода
     * @param to дата конца периода
     * @return список всех транзакций, относящихся к этому периоду
     * @throws IllegalArgumentException если {@code from} идет после {@code to} хронологически
     */
    public List<TransactionDto> getTransactionsByDateBetween(LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From-date cannot be after to-date");
        }
        logger.debug("getTransactionsByDateBetween() from: {}; to: {}", from.toString(), to.toString());
        return transactionRepository.findAllByUserIdAndDateBetween(
                getCurrentUserId(), from, to)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает транзакцию по ее идентификатору
     * @param transactionId идентификатор транзакции
     * @return дто, содержащее данные искомой транзакции
     * @throws NotFoundException если транзакция с указанным {@code transactionId} не существует
     */
    public TransactionDto getTransactionById(UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        logger.debug("getTransactionById() with id: {}", transactionId.toString());
        return toDto(transaction);
    }

    /**
     * Изменяет транзакция с указанным идентификатором
     * @param transactionId идентификатор транзакции, подлежащей изменению
     * @param request запрос, содержащий данные для изменения
     * @return дто, с данными измененной транзакции
     * @throws NotFoundException если транзакции с данным {@code transactionId} не существует
     */
    @Transactional
    public TransactionDto updateTransaction(UUID transactionId, TransactionRequest request) {
        Long userId = getCurrentUserId();

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (request.amount() != null) {
            transaction.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        }

        if (request.date() != null) {
            transaction.setDate(request.date());
        }

        transaction.setPlanned(request.isPlanned());

        if (request.description() != null) {
            transaction.setDescription(request.description());
        }

        if (request.categoryId() != null) {
            Category category = getCategory(request.categoryId(), userId);
            transaction.setCategory(category);
        }
        logger.debug("updateTransaction() with id: {}", transactionId.toString());
        return toDto(transactionRepository.save(transaction));
    }

    /**
     * Удаляет транзакцию с указанным идентификатором
     * @param transactionId идентификатор транзакции, подлежащей удалению
     * @throws NotFoundException если транзакции с данным {@code transactionId} не существует
     */
    @Transactional
    public void deleteTransaction(UUID transactionId) {

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        logger.debug("deleteTransaction() with id: {}", transactionId.toString());
        transactionRepository.delete(transaction);
    }

    /**
     * Служебный метод возвращающий текущего пользователя
     * @return текущий пользователь
     */
    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    /**
     * Служебный метод возвращающий идентификатор текущего пользователя
     * @return идентификатор пользователя
     */
    private Long getCurrentUserId() {
        return userService.getCurrentUser().getId();
    }

    /**
     * Службный метод получающий категорию по ее идентификатору и идентификатору пользователя
     * @param categoryId идентфикатор категории
     * @param userId идентификатор пользователя
     * @return найденная категория
     * @throws NotFoundExcption если категории с {@code categoryId} и/или {@code userId} не существует
     */
    private Category getCategory(UUID categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    /**
     * Служебный метод преобразующий транзакцию-сущность в транзакцию-дто
     * @param transaction сущность для преобразования
     * @return дто, содержащее данные сущности
     */
    private TransactionDto toDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getDescription(),
                transaction.isPlanned());
    }

}

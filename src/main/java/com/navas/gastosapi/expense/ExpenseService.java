package com.navas.gastosapi.expense;

import com.navas.gastosapi.category.CategoryRepository;
import com.navas.gastosapi.common.NotFoundException;
import com.navas.gastosapi.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories) {
        this.expenses = expenses;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(Long userId, LocalDate from, LocalDate to, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "spentOn").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Expense> result = expenses.findByUserIdAndSpentOnBetween(
                userId,
                from != null ? from : MIN_DATE,
                to != null ? to : MAX_DATE,
                pageable);
        return PageResponse.from(result.map(ExpenseResponse::from));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long userId, Long id) {
        return ExpenseResponse.from(find(userId, id));
    }

    @Transactional
    public ExpenseResponse create(Long userId, ExpenseRequest request) {
        assertCategoryOwned(userId, request.categoryId());
        Expense expense = new Expense(userId, request.categoryId(), request.amount(),
                request.description(), request.spentOn());
        return ExpenseResponse.from(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long id, ExpenseRequest request) {
        Expense expense = find(userId, id);
        assertCategoryOwned(userId, request.categoryId());
        expense.update(request.categoryId(), request.amount(), request.description(), request.spentOn());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        expenses.delete(find(userId, id));
    }

    private Expense find(Long userId, Long id) {
        return expenses.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Despesa não encontrada"));
    }

    private void assertCategoryOwned(Long userId, Long categoryId) {
        if (categoryId != null && !categories.existsByIdAndUserId(categoryId, userId)) {
            throw new NotFoundException("Categoria não encontrada");
        }
    }
}

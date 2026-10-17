package com.navas.gastosapi.category;

import com.navas.gastosapi.common.ConflictException;
import com.navas.gastosapi.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Toda consulta filtra pelo userId do token. Recurso de outro usuario responde
 * 404 (e nao 403), para nao revelar que o id existe. Isso evita IDOR.
 */
@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categories.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByUserIdAndName(userId, name)) {
            throw new ConflictException("Categoria já existe");
        }
        return CategoryResponse.from(categories.save(new Category(userId, name)));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        Category category = find(userId, id);
        String name = request.name().trim();
        if (!category.getName().equals(name) && categories.existsByUserIdAndName(userId, name)) {
            throw new ConflictException("Categoria já existe");
        }
        category.setName(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        categories.delete(find(userId, id));
    }

    private Category find(Long userId, Long id) {
        return categories.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
    }
}

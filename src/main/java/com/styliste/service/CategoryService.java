package com.styliste.service;

import com.styliste.entity.Category;
import com.styliste.entity.SubCategory;
import com.styliste.repository.CategoryRepository;
import com.styliste.repository.SubCategoryRepository;
import com.styliste.exception.ResourceNotFoundException; // Assuming you have this
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    // --- CATEGORY OPERATIONS ---

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("Category already exists: " + name);
        }
        Category category = Category.builder()
                .name(name)
                .description(description)
                .isActive(true)
                .build();
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }

    // --- SUB-CATEGORY OPERATIONS ---

    public SubCategory createSubCategory(Long categoryId, String name, String description) {
        // 1. Find Parent
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

        // 2. Create Child
        SubCategory subCategory = SubCategory.builder()
                .name(name)
                .description(description)
                .category(category)
                .build();

        // 3. Save Child directly (Returns the ID immediately)
        return subCategoryRepository.save(subCategory);
    }

    public void deleteSubCategory(Long subId) {
        subCategoryRepository.deleteById(subId);
    }
}
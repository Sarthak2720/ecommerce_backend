package com.styliste.service;

import com.styliste.entity.Category;
import com.styliste.entity.SubCategory;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceAlreadyExistsException;
import com.styliste.repository.CategoryRepository;
import com.styliste.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    // --- CATEGORY OPERATIONS ---

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }
    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new ResourceAlreadyExistsException("Category named '" + name + "' already exists.");
        }
        Category category = Category.builder()
                .name(name)
                .description(description)
                .isActive(true)
                .build();
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, String name, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // If the name is changing, check for duplicates
        if (name != null && !category.getName().equalsIgnoreCase(name)) {
            if (categoryRepository.existsByName(name)) {
                throw new ResourceAlreadyExistsException("Category name '" + name + "' is already taken.");
            }
            category.setName(name);
        }

        if (description != null) category.setDescription(description);

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // Scenario: Blocking delete if children exist
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new BadRequestException("Cannot delete category. An associated sub-category is present ("
                    + category.getSubCategories().size() + " found).");
        }

        categoryRepository.delete(category);
    }
    // --- SUB-CATEGORY OPERATIONS ---
    public SubCategory createSubCategory(Long categoryId, String name, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Category not found."));

        if (subCategoryRepository.existsByNameAndCategoryId(name, categoryId)) {
            throw new ResourceAlreadyExistsException("Sub-category '" + name + "' already exists under " + category.getName());
        }

        SubCategory subCategory = SubCategory.builder()
                .name(name)
                .description(description)
                .category(category)
                .build();

        return subCategoryRepository.save(subCategory);
    }

    public SubCategory updateSubCategory(Long subId, String name, String description) {
        SubCategory sub = subCategoryRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found with ID: " + subId));

        // Logic check: Duplicates within the same category
        if (name != null && !sub.getName().equalsIgnoreCase(name)) {
            if (subCategoryRepository.existsByNameAndCategoryId(name, sub.getCategory().getId())) {
                throw new ResourceAlreadyExistsException("Another sub-category named '" + name + "' already exists in this category.");
            }
            sub.setName(name);
        }

        if (description != null) sub.setDescription(description);

        return subCategoryRepository.save(sub);
    }

    public void deleteSubCategory(Long subId) {
        // 1. Find the sub-category first to get its name
        SubCategory subCategory = subCategoryRepository.findById(subId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found."));

        // 2. Check if any products are using this sub-category
        // We check by name because the Product entity stores subcategory as a String
        boolean hasProducts = productRepository.existsBySubcategory(subCategory.getName());

        if (hasProducts) {
            // 3. Throw the specific error message for the frontend to display
            throw new BadRequestException("Cannot delete sub-category '" + subCategory.getName() +
                    "'. There are existing products associated with it.");
        }

        // 4. If no products, proceed with deletion
        subCategoryRepository.delete(subCategory);
    }

}
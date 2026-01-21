package com.styliste.controller;

import com.styliste.entity.Category;
import com.styliste.entity.SubCategory;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.CategoryRepository;
import com.styliste.repository.SubCategoryRepository;
import com.styliste.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    // Public: Everyone can see categories
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // Admin Only: Create Category
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String desc = payload.get("description");
        return ResponseEntity.ok(categoryService.createCategory(name, desc));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(categoryService.updateCategory(id, payload.get("name"), payload.get("description")));
    }

    // Admin Only: Delete Category
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // Admin Only: Add SubCategory to a Category
    @PostMapping("/{categoryId}/subcategories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubCategory> createSubCategory(
            @PathVariable("categoryId") Long categoryId,      // 1. Get ID from URL
            @RequestBody Map<String, String> payload          // 2. Get JSON Body ({name: "...", desc: "..."})
    ) {
        String name = payload.get("name");
        String desc = payload.get("description");

        // 3. Call the Service
        return ResponseEntity.ok(categoryService.createSubCategory(categoryId, name, desc));
    }

    @PutMapping("/subcategories/{subId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubCategory> updateSubCategory(@PathVariable Long subId, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(categoryService.updateSubCategory(subId, payload.get("name"), payload.get("description")));
    }

    // Admin Only: Delete SubCategory
    @DeleteMapping("/subcategories/{subId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubCategory(@PathVariable Long subId) {
        categoryService.deleteSubCategory(subId);
        return ResponseEntity.noContent().build();
    }
}
package com.styliste.repository;

import com.styliste.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    List<Product> findBySubcategory(String subcategory);

    List<Product> findByCategoryAndSubcategory(String category, String subcategory);

    boolean existsBySubcategory(String subcategoryName);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:subcategory IS NULL OR p.subcategory = :subcategory) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:searchQuery IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) AND " +
            "p.isActive = true")
    Page<Product> searchProducts(
            @Param("category") String category,
            @Param("subcategory") String subcategory,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("searchQuery") String searchQuery,
            Pageable pageable
    );

    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();
}

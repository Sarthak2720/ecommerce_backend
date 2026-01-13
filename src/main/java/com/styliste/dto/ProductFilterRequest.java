package com.styliste.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {
    private String category;
    private String subcategory;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String searchQuery;
    private String sortBy; // "name", "price", "createdAt"
    private String sortOrder; // "ASC", "DESC"
    private Integer page;
    private Integer pageSize;
}

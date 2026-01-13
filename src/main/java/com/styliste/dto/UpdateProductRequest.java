package com.styliste.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRequest {

    @Size(min = 3, max = 200, message = "Product name must be between 3 and 200 characters")
    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "Sale price must be greater than or equal to 0")
    private BigDecimal salePrice;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private String category;
    private String subcategory;
    private List<String> images;
    private List<String> videos;
    private List<ProductAttributeDTO> attributes;
    private Boolean isActive;
}

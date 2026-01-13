package com.styliste.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stock;
    private String category;
    private String subcategory;
    private List<String> images;
    private List<String> videos;
    private List<ProductAttributeDTO> attributes;
    private Boolean isActive;
}

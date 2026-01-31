package com.styliste.dto;


import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistDTO {
    private Long id;
    private Long userId;
    private List<ProductDTO> products; // Reuse your existing ProductDTO here
}
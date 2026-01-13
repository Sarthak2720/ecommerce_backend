package com.styliste.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeDTO {
    private String type;
    private String value;
}

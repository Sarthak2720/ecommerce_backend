package com.styliste.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {
    @Column(name = "attr_type", length = 50)
    private String type; // Size, Color

    @Column(name = "attr_value", length = 100)
    private String value; // S, M, L, XL, etc.
}

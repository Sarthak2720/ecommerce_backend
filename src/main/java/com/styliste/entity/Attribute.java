package com.styliste.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attributes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"attr_type", "attr_value"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attr_type", length = 50)
    private String type; // "COLOR" or "SIZE"

    @Column(name = "attr_value", length = 100)
    private String value; // "Red", "XL", etc.
}

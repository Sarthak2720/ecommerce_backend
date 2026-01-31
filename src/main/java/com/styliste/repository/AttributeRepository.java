package com.styliste.repository;

import com.styliste.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    Optional<Attribute> findByTypeAndValue(String type, String value);

    @Query("SELECT COUNT(p) FROM Product p JOIN p.attributes a WHERE a.id = :attributeId")
    long countProductsUsingAttribute(@Param("attributeId") Long attributeId);

    // New method to get unique values directly
    @Query("SELECT DISTINCT a.value FROM Attribute a WHERE a.type = :type")
    List<String> findUniqueValuesByType(@Param("type") String type);
}

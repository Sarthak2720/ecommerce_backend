package com.styliste.repository;

import com.styliste.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    // This will provide save(), findById(), and saveAll() automatically
}
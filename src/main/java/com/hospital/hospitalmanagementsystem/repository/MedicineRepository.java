package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByCategory(String category);
    List<Medicine> findByExpiryDateBefore(LocalDate date);
    List<Medicine> findByStockLessThan(int minStock);
    List<Medicine> findAllByActiveTrue();
    List<Medicine> findByCategoryAndActiveTrue(String category);
    List<Medicine> findByNameContainingIgnoreCase(String name);

    // Search method with pagination
    Page<Medicine> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category, Pageable pageable);

    // Fix: Use a custom query to handle the underscore in field names
    @Query("SELECT m FROM Medicine m WHERE m.name LIKE %:term% OR m.generic_name LIKE %:term% OR m.brand LIKE %:term%")
    List<Medicine> findByNameContainingOrGeneric_nameContainingOrBrandContaining(@Param("term") String term);

}
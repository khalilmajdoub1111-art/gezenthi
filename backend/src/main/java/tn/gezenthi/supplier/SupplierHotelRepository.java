package tn.gezenthi.supplier;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierHotelRepository extends JpaRepository<SupplierHotel, UUID> {

    List<SupplierHotel> findBySupplierCode(String supplierCode);

    long countBySupplierCodeAndActiveTrue(String supplierCode);

    @Query("""
            select h from SupplierHotel h
            where h.supplierCode = :supplier and h.active = true
              and lower(h.cityName) = lower(:city)
            """)
    List<SupplierHotel> findActiveInCity(@Param("supplier") String supplier, @Param("city") String city);
}

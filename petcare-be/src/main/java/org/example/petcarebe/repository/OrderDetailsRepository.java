package org.example.petcarebe.repository;

import org.example.petcarebe.model.OrderDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailsRepository extends JpaRepository<OrderDetails, Long > {

    @Query("SELECT od.productDetails, SUM(od.quantity) as totalSold " +
            "FROM OrderDetails od " +
            "GROUP BY od.productDetails " +
            "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts(Pageable pageable);


}

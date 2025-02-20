package org.example.petcarebe.repository;

import org.example.petcarebe.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserUserId(Long userId);
    List<Orders> findByStatusOrder_StatusId(Long statusId);
//    List<Orders> findByPaymentStatus(Long paymentStatus);
    List<Orders> findByType(String type);


}

package org.example.petcarebe.repository;

import org.example.petcarebe.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_UserId(Long userId);
    List<Order> findByStatusOrder_StatusId(Long statusId);
    List<Order> findByPaymentStatus(int paymentStatus);
    List<Order> findByType(int type);
}

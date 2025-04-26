package org.example.petcarebe.repository;

import org.example.petcarebe.model.OrderVetDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderVetDetailRepository extends JpaRepository<OrderVetDetail, Long> {

    // Tìm tất cả OrderVetDetail theo orderId
    List<OrderVetDetail> findByOrderId_OrderId(Long orderId);


}
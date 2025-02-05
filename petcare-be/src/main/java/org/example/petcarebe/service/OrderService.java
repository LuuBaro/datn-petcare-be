package org.example.petcarebe.service;

import org.example.petcarebe.model.Orders;
import org.example.petcarebe.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public List<Orders> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Orders> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Orders> getOrdersByUserId(Long userId) {
        return orderRepository.findByUser_UserId(userId);
    }

    public List<Orders> getOrdersByStatus(Long statusId) {
        return orderRepository.findByStatusOrder_StatusId(statusId);
    }

    public List<Orders> getOrdersByPaymentStatus(int paymentStatus) {
        return orderRepository.findByPaymentStatus(paymentStatus);
    }

    @Transactional
    public Orders createOrder(Orders orders) {
        // Calculate shipping cost based on weight or other factors
        calculateShippingCost(orders);

        // Calculate total amount
        calculateTotalAmount(orders);

        // Calculate points earned (if applicable)
        calculatePointsEarned(orders);

        return orderRepository.save(orders);
    }

    @Transactional
    public Orders updateOrder(Orders orders) {
        if (!orderRepository.existsById(orders.getOrderId())) {
            throw new RuntimeException("Order not found");
        }
        return orderRepository.save(orders);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    private void calculateShippingCost(Orders orders) {
        // Implement shipping cost calculation logic
        // This could be based on weight, distance, shipping method, etc.
    }

    private void calculateTotalAmount(Orders orders) {
        // Calculate total amount including items, shipping, discounts, etc.
        float total = 0;
        // Add logic to calculate total from order details
        orders.setTotalAmount(total);
    }

    private void calculatePointsEarned(Orders orders) {
        // Implement points calculation logic based on order total
        // This could vary based on user tier, promotions, etc.
        int points = (int)(orders.getTotalAmount() / 100); // Example: 1 point per $100
        orders.setPointEarned(points);
    }
}
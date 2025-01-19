package org.example.petcarebe.service;

import org.example.petcarebe.model.Order;
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

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUser_UserId(userId);
    }

    public List<Order> getOrdersByStatus(Long statusId) {
        return orderRepository.findByStatusOrder_StatusId(statusId);
    }

    public List<Order> getOrdersByPaymentStatus(int paymentStatus) {
        return orderRepository.findByPaymentStatus(paymentStatus);
    }

    @Transactional
    public Order createOrder(Order order) {
        // Calculate shipping cost based on weight or other factors
        calculateShippingCost(order);

        // Calculate total amount
        calculateTotalAmount(order);

        // Calculate points earned (if applicable)
        calculatePointsEarned(order);

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(Order order) {
        if (!orderRepository.existsById(order.getOrderId())) {
            throw new RuntimeException("Order not found");
        }
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    private void calculateShippingCost(Order order) {
        // Implement shipping cost calculation logic
        // This could be based on weight, distance, shipping method, etc.
    }

    private void calculateTotalAmount(Order order) {
        // Calculate total amount including items, shipping, discounts, etc.
        float total = 0;
        // Add logic to calculate total from order details
        order.setTotalAmount(total);
    }

    private void calculatePointsEarned(Order order) {
        // Implement points calculation logic based on order total
        // This could vary based on user tier, promotions, etc.
        int points = (int)(order.getTotalAmount() / 100); // Example: 1 point per $100
        order.setPointEarned(points);
    }
}
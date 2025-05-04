package org.example.petcarebe.service;

import org.example.petcarebe.enums.PaymentMethod;
import org.example.petcarebe.model.Appointment;
import org.example.petcarebe.model.Orders;
import org.example.petcarebe.model.OrderSpa;
import org.example.petcarebe.model.Pet;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AppointmentRepository;
import org.example.petcarebe.repository.OrderRepository;
import org.example.petcarebe.repository.OrderSpaRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderSpaService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSpaRepository orderSpaRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Orders createSpaOrder(Long appointmentId, Long userId, String paymentMethod, String paymentChannel) {
        // Tìm Appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Lịch hẹn không tồn tại: " + appointmentId));

        // Ghi log trạng thái hiện tại của Appointment
        System.out.println("Creating spa order for appointment #" + appointmentId + ", current status: " + appointment.getStatus());

        // Kiểm tra dữ liệu Appointment
        if (appointment.getTotalAmount() <= 0) {
            System.err.println("Tổng số tiền của lịch hẹn #" + appointmentId + " không hợp lệ: " + appointment.getTotalAmount());
            throw new IllegalStateException("Tổng số tiền của lịch hẹn #" + appointmentId + " không hợp lệ");
        }

        List<Pet> pets = appointment.getPets();
        if (pets == null || pets.isEmpty()) {
            System.err.println("Lịch hẹn #" + appointmentId + " không có thú cưng nào để tạo hóa đơn");
            throw new IllegalStateException("Lịch hẹn #" + appointmentId + " không có thú cưng nào để tạo hóa đơn");
        }

        // Tìm User (nhân viên thực hiện)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại: " + userId));

        // Xác định paymentMethod
        PaymentMethod finalPaymentMethod = determinePaymentMethod(paymentMethod);

        // Tạo Orders
        Orders order = new Orders();
        order.setOrderDate(new Date());
        order.setPaymentMethod(finalPaymentMethod.name());
        order.setPaymentStatus("Đã thanh toán");
        order.setType("SPA");
        order.setUser(user);
        order.setShippingCost(0);
        order.setTotalAmount((float) appointment.getTotalAmount());
        order.setPointUsed(0);
        order.setPointEarned(0);
        order.setMomoOrderId(null); // Không áp dụng MoMo trong trường hợp này
        order.setMomoTransId(null);
        order.setMomoAmount(null);
        order.setShippingAddress(null); // Không cần địa chỉ giao hàng cho spa

        // Lưu Orders
        Orders savedOrder = orderRepository.save(order);

        // Tạo OrderSpa cho từng Pet
        List<OrderSpa> orderSpaList = new ArrayList<>();
        for (Pet pet : pets) {
            if (pet.getPrice() <= 0) {
                System.err.println("Thú cưng #" + pet.getId() + " trong lịch hẹn #" + appointmentId + " có giá không hợp lệ: " + pet.getPrice());
                throw new IllegalStateException("Thú cưng #" + pet.getId() + " trong lịch hẹn #" + appointmentId + " có giá không hợp lệ");
            }

            OrderSpa orderSpa = new OrderSpa();
            orderSpa.setOrder(savedOrder);
            orderSpa.setPet(pet);
            orderSpa.setPrice((float) pet.getPrice());
            orderSpaList.add(orderSpa);
        }

        // Lưu tất cả OrderSpa
        orderSpaRepository.saveAll(orderSpaList);

        return savedOrder;
    }

    private PaymentMethod determinePaymentMethod(String requestedPaymentMethod) {
        // Parse trực tiếp requestedPaymentMethod thành PaymentMethod
        try {
            return PaymentMethod.valueOf(requestedPaymentMethod);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid payment method: " + requestedPaymentMethod + ". Defaulting to ONLINE.");
            return PaymentMethod.ONLINE;
        }
    }
}
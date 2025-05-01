package org.example.petcarebe.dto;

public class AppointmentResponse {
    private Long appointmentId;
    private String customerName;
    private String phone;
    private String date;
    private String time;
    private double paidAmount;
    private double totalAmount;
    private double depositAmount;
    private int petCount;
    private String status;
    private String message;
    // Trường mới cho hoàn tiền
    private double refundAmount;
    private double nonRefundedDeposit;
    private String refundStatus;
    private String refundMethod;
    private String refundNote;
    private String cancelReason;

    // Constructor cho createAppointment
    public AppointmentResponse(Long appointmentId, String status, String message) {
        this.appointmentId = appointmentId;
        this.status = status;
        this.message = message;
    }

    // Constructor cho getPendingAppointments
    public AppointmentResponse(Long appointmentId, String customerName, String phone, String date, String time,
                               double paidAmount, double totalAmount, double depositAmount, int petCount) {
        this.appointmentId = appointmentId;
        this.customerName = customerName;
        this.phone = phone;
        this.date = date;
        this.time = time;
        this.paidAmount = paidAmount;
        this.totalAmount = totalAmount;
        this.depositAmount = depositAmount;
        this.petCount = petCount;
    }

    // Constructor mới cho các chức năng liên quan đến hoàn tiền
    public AppointmentResponse(Long appointmentId, String customerName, String phone, String date, String time,
                               double refundAmount, double nonRefundedDeposit, String refundStatus, String refundMethod,
                               String refundNote, String cancelReason) {
        this.appointmentId = appointmentId;
        this.customerName = customerName;
        this.phone = phone;
        this.date = date;
        this.time = time;
        this.refundAmount = refundAmount;
        this.nonRefundedDeposit = nonRefundedDeposit;
        this.refundStatus = refundStatus;
        this.refundMethod = refundMethod;
        this.refundNote = refundNote;
        this.cancelReason = cancelReason;
    }

    // Getters và setters
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(double depositAmount) { this.depositAmount = depositAmount; }
    public int getPetCount() { return petCount; }
    public void setPetCount(int petCount) { this.petCount = petCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }
    public double getNonRefundedDeposit() { return nonRefundedDeposit; }
    public void setNonRefundedDeposit(double nonRefundedDeposit) { this.nonRefundedDeposit = nonRefundedDeposit; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public String getRefundMethod() { return refundMethod; }
    public void setRefundMethod(String refundMethod) { this.refundMethod = refundMethod; }
    public String getRefundNote() { return refundNote; }
    public void setRefundNote(String refundNote) { this.refundNote = refundNote; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
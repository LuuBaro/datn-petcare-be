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

    // Constructor for createAppointment
    public AppointmentResponse(Long appointmentId, String status, String message) {
        this.appointmentId = appointmentId;
        this.status = status;
        this.message = message;
    }

    // Constructor for getPendingAppointments
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

    // Getters and setters
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
}
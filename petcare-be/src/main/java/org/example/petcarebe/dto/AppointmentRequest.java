package org.example.petcarebe.dto;

import java.util.List;

public class AppointmentRequest {
    private String date;
    private String time;
    private String currentDate;
    private String currentTime;
    private String customerName;
    private String phone;
    private String paymentType;
    private double depositAmount;
    private double totalAmount;
    private double paidAmount;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentChannel;
    private String note; // Thêm field note
    private List<PetRequest> pets;

    // Getters và Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public double getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(double depositAmount) {
        this.depositAmount = depositAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<PetRequest> getPets() {
        return pets;
    }

    public void setPets(List<PetRequest> pets) {
        this.pets = pets;
    }

    public static class PetRequest {
        private String name;
        private String petType;
        private Long petServiceId;
        private Long petWeightId;
        private String note;
        private double price;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPetType() {
            return petType;
        }

        public void setPetType(String petType) {
            this.petType = petType;
        }

        public Long getPetServiceId() {
            return petServiceId;
        }

        public void setPetServiceId(Long petServiceId) {
            this.petServiceId = petServiceId;
        }

        public Long getPetWeightId() {
            return petWeightId;
        }

        public void setPetWeightId(Long petWeightId) {
            this.petWeightId = petWeightId;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    @Override
    public String toString() {
        return "AppointmentRequest{" +
                "date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", currentDate='" + currentDate + '\'' +
                ", currentTime='" + currentTime + '\'' +
                ", customerName='" + customerName + '\'' +
                ", phone='" + phone + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", depositAmount=" + depositAmount +
                ", totalAmount=" + totalAmount +
                ", paidAmount=" + paidAmount +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentChannel='" + paymentChannel + '\'' +
                ", note='" + note + '\'' +
                ", pets=" + pets +
                '}';
    }
}
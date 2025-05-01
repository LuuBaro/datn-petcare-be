package org.example.petcarebe.dto;

public class PetResponse {
    private Long id;
    private String name;
    private String petType;
    private Float age; // Thay đổi từ Integer thành Float
    private String weightRange;
    private String serviceName;
    private double price; // Thay đổi từ Double thành double

    public PetResponse(Long id, String name, String petType, Float age, String weightRange, String serviceName, double price) {
        this.id = id;
        this.name = name;
        this.petType = petType;
        this.age = age;
        this.weightRange = weightRange;
        this.serviceName = serviceName;
        this.price = price;
    }

    // Getters và setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Float getAge() {
        return age;
    }

    public void setAge(Float age) {
        this.age = age;
    }

    public String getWeightRange() {
        return weightRange;
    }

    public void setWeightRange(String weightRange) {
        this.weightRange = weightRange;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
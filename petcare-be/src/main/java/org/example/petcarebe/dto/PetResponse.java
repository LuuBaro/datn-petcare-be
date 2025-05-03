package org.example.petcarebe.dto;

public class PetResponse {
    private Long id;
    private String name;
    private String petType;
    private Float age;
    private String weightRange;
    private String serviceName;
    private Long petServiceId;
    private double price;
    private Integer weightUpdateCount; // Thêm trường weightUpdateCount

    public PetResponse(Long id, String name, String petType, Float age, String weightRange, String serviceName, double price) {
        this.id = id;
        this.name = name;
        this.petType = petType;
        this.age = age;
        this.weightRange = weightRange;
        this.serviceName = serviceName;
        this.price = price;
    }

    public PetResponse(Long id, String name, String petType, Float age, String weightRange, String serviceName, Long petServiceId, double price) {
        this.id = id;
        this.name = name;
        this.petType = petType;
        this.age = age;
        this.weightRange = weightRange;
        this.serviceName = serviceName;
        this.petServiceId = petServiceId;
        this.price = price;
    }

    public PetResponse(Long id, String name, String petType, Float age, String weightRange, String serviceName, Long petServiceId, double price, Integer weightUpdateCount) {
        this.id = id;
        this.name = name;
        this.petType = petType;
        this.age = age;
        this.weightRange = weightRange;
        this.serviceName = serviceName;
        this.petServiceId = petServiceId;
        this.price = price;
        this.weightUpdateCount = weightUpdateCount;
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

    public Long getPetServiceId() {
        return petServiceId;
    }

    public void setPetServiceId(Long petServiceId) {
        this.petServiceId = petServiceId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getWeightUpdateCount() {
        return weightUpdateCount;
    }

    public void setWeightUpdateCount(Integer weightUpdateCount) {
        this.weightUpdateCount = weightUpdateCount;
    }
}
package org.example.petcarebe.dto;

import lombok.Data;

@Data
public class PetResponse {
    private Long id;
    private String namePet;
    private String petType;
    private double age;
    private String weightRange;
    private String serviceName;
    private double price;

    public PetResponse(Long id, String namePet, String petType, double age, String weightRange, String serviceName, double price) {
        this.id = id;
        this.namePet = namePet;
        this.petType = petType;
        this.age = age;
        this.weightRange = weightRange;
        this.serviceName = serviceName;
        this.price = price;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNamePet() { return namePet; }
    public void setNamePet(String namePet) { this.namePet = namePet; }
    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }
    public double getAge() { return age; }
    public void setAge(double age) { this.age = age; }
    public String getWeightRange() { return weightRange; }
    public void setWeightRange(String weightRange) { this.weightRange = weightRange; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
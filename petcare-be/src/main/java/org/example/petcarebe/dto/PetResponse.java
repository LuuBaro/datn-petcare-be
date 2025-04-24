package org.example.petcarebe.dto;

public class PetResponse {
    private Long id;
    private String name;
    private String type;
    private Float age;
    private String weight;
    private String service;
    private Float price;

    public PetResponse(Long id, String name, String type, Float age, String weight, String service, Float price) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.age = age;
        this.weight = weight;
        this.service = service;
        this.price = price;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Float getAge() { return age; }
    public void setAge(Float age) { this.age = age; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }
}
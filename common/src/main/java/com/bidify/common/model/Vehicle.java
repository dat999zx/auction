package com.bidify.common.model;

/**
 * Vehicle - Item category for vehicles (cars, motorcycles, etc.)
 */
public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;
    
    private String vehicleType;
    private String manufacturer; // nhà sx
    private String model; // mô hình
    private int yearManufactured; // năm sx
    private double mileage; // số dặm đã đi
    private String fuelType; // loại nhiên liệu
    private String color; 
    private boolean hasAccidents; // Tai nạn
    private String registrationNumber; // số đăng ký
    
    public Vehicle() {
        super();
    }
    
    public Vehicle(String name, String description, double startingPrice, String sellerId) {
        super(name, description, startingPrice, sellerId);
    }
    
    @Override
    public String getItemType() {
        return "VEHICLE";
    }
    
    public String getVehicleType() {
        return vehicleType;
    }
    
    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public int getYearManufactured() {
        return yearManufactured;
    }
    
    public void setYearManufactured(int yearManufactured) {
        this.yearManufactured = yearManufactured;
    }
    
    public double getMileage() {
        return mileage;
    }
    
    public void setMileage(double mileage) {
        this.mileage = mileage;
    }
    
    public String getFuelType() {
        return fuelType;
    }
    
    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public boolean hasAccidents() {
        return hasAccidents;
    }
    
    public void setHasAccidents(boolean hasAccidents) {
        this.hasAccidents = hasAccidents;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}

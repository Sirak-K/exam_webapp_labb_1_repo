package com.karis;

public class WeatherDTO {

    private String validTime;
    private double temperature;

    public WeatherDTO(String validTime, double temperature) {
        this.validTime = validTime;
        this.temperature = temperature;
    }

    public String getValidTime() {
        return validTime;
    }

    public double getTemperature() {
        return temperature;
    }
}

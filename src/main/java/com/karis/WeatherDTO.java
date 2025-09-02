package com.karis;

/**
 * WeatherDTO - Data Transfer Object för väderinformation.
 *
 * Används för att skicka validerad data mellan backend och frontend. Record ger
 * immutabla fält, automatiska getters, equals, hashCode och toString.
 */
public record WeatherDTO(String validTime, double temperature) {

}

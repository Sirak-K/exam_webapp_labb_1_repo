package com.karis;

/**
 * DTO för väderinformation.
 *
 * För att skicka validerad data mellan backend och frontend. Record bidrar med
 * immutabla fält, automatiska getters, equals, hashCode och toString.
 */
public record WeatherDTO(String validTime, double temperature) {

}

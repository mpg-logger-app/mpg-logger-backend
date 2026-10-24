package com.chandan.mpgloggerbackend.fuellog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * JPA version of the SwiftData @Model FuelLog.
 * The Swift app picks only a date (no time), so LocalDate is used.
 */
@Entity
@Table(name = "fuel_log")
public class FuelLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "date" is avoided as a column name because it is an SQL keyword.
    @Column(name = "fuel_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private double previousOdometer;

    @Column(nullable = false)
    private double odometer;

    @Column(nullable = false)
    private double gallons;

    @Column(nullable = false)
    private double pricePerGallon;

    /** Required by JPA. */
    protected FuelLog() {
    }

    public FuelLog(LocalDate date, double previousOdometer, double odometer, double gallons, double pricePerGallon) {
        this.date = date;
        this.previousOdometer = previousOdometer;
        this.odometer = odometer;
        this.gallons = gallons;
        this.pricePerGallon = pricePerGallon;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getPreviousOdometer() {
        return previousOdometer;
    }

    public double getOdometer() {
        return odometer;
    }

    public double getGallons() {
        return gallons;
    }

    public double getPricePerGallon() {
        return pricePerGallon;
    }

    public double getMilesDriven() {
        return FuelLogRules.milesDriven(previousOdometer, odometer);
    }

    public double getMpg() {
        return FuelLogRules.mpg(previousOdometer, odometer, gallons);
    }

    public double getTotalCost() {
        return FuelLogRules.totalCost(gallons, pricePerGallon);
    }
}

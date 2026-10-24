package com.chandan.mpgloggerbackend.fuellog;

import com.chandan.mpgloggerbackend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // Whose log this is. Lazy: the API never needs the owner's details, only to filter by them.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

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

    /**
     * A new fill-up. The values are expected to have passed {@link FuelLogRules#validate}.
     *
     * @param user             who the log belongs to
     * @param date             the day of the fill-up
     * @param previousOdometer odometer reading where this tank started (the previous fill-up)
     * @param odometer         odometer reading at this fill-up
     * @param gallons          gallons pumped
     * @param pricePerGallon   price paid per gallon, in dollars
     */
    public FuelLog(AppUser user, LocalDate date, double previousOdometer, double odometer, double gallons, double pricePerGallon) {
        this.user = user;
        this.date = date;
        this.previousOdometer = previousOdometer;
        this.odometer = odometer;
        this.gallons = gallons;
        this.pricePerGallon = pricePerGallon;
    }

    /**
     * Applies an edit. previousOdometer only changes for the oldest log (see FuelLogService.update).
     *
     * @param date             the new fill-up day
     * @param previousOdometer the new starting reading
     * @param odometer         the new reading at this fill-up
     * @param gallons          the new gallons pumped
     * @param pricePerGallon   the new price per gallon
     */
    public void update(LocalDate date, double previousOdometer, double odometer, double gallons, double pricePerGallon) {
        this.date = date;
        this.previousOdometer = previousOdometer;
        this.odometer = odometer;
        this.gallons = gallons;
        this.pricePerGallon = pricePerGallon;
    }

    /**
     * Keeps the next log starting where an edited log now ends.
     *
     * @param previousOdometer the edited log's new odometer reading
     */
    public void setPreviousOdometer(double previousOdometer) {
        this.previousOdometer = previousOdometer;
    }

    /** @return the database id, or null before the log is saved */
    public Long getId() {
        return id;
    }

    /** @return the day of the fill-up */
    public LocalDate getDate() {
        return date;
    }

    /** @return the odometer reading where this tank started */
    public double getPreviousOdometer() {
        return previousOdometer;
    }

    /** @return the odometer reading at this fill-up */
    public double getOdometer() {
        return odometer;
    }

    /** @return gallons pumped */
    public double getGallons() {
        return gallons;
    }

    /** @return price paid per gallon, in dollars */
    public double getPricePerGallon() {
        return pricePerGallon;
    }

    /** @return miles driven on this tank (odometer minus previous odometer) */
    public double getMilesDriven() {
        return FuelLogRules.milesDriven(previousOdometer, odometer);
    }

    /** @return miles per gallon for this tank, or 0 if no gallons were recorded */
    public double getMpg() {
        return FuelLogRules.mpg(previousOdometer, odometer, gallons);
    }

    /** @return what this fill-up cost (gallons times price per gallon) */
    public double getTotalCost() {
        return FuelLogRules.totalCost(gallons, pricePerGallon);
    }
}

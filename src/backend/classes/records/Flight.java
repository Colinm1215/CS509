package backend.classes.records;

import backend.interfaces.FlightInterface;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class Flight implements FlightInterface {
    private final int id;
    private final String flightNumber;
    private final String departureAirport;
    private final String arrivalAirport;
    private final Timestamp departureTime;
    private final Timestamp arrivalTime;
    private int seatsFree;

    public Flight(int id, String flightNumber, String departureAirport, String arrivalAirport, Timestamp departureTime, Timestamp arrivalTime) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.seatsFree = 100;
    }

    public Flight(ResultSet rs) {
        try {
            this.id = rs.getInt("Id");
            this.flightNumber = rs.getString("FlightNumber");
            this.departureAirport = rs.getString("DepartAirport");
            this.arrivalAirport = rs.getString("ArriveAirport");
            this.departureTime = rs.getTimestamp("DepartDateTime");
            this.arrivalTime = rs.getTimestamp("ArriveDateTime");
            this.seatsFree = 100;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public Timestamp getDepartureTime() {
        return departureTime;
    }

    public Timestamp getArrivalTime() {
        return arrivalTime;
    }

    public int getSeatsFree()
    {
        return seatsFree;
    }

    public void reserved(){
        seatsFree = seatsFree - 1;
    }

    @Override
    public String toString() {
        return """
                Flight{id=%d,
                flightNumber='%s',
                departureAirport='%s',
                arrivalAirport='%s',
                departureTime=%s,
                arrivalTime=%s
                seatsFree=%d}""".formatted(id, flightNumber, departureAirport, arrivalAirport, departureTime, arrivalTime, seatsFree);
    }
}

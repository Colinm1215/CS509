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

    public Flight(int id, String flightNumber, String departureAirport, String arrivalAirport, Timestamp departureTime, Timestamp arrivalTime) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public Flight(ResultSet rs) {
        try {
            this.id = rs.getInt("Id");
            this.flightNumber = rs.getString("FlightNumber");
            this.departureAirport = rs.getString("DepartAirport");
            this.arrivalAirport = rs.getString("ArriveAirport");
            this.departureTime = rs.getTimestamp("DepartDateTime");
            this.arrivalTime = rs.getTimestamp("ArriveDateTime");
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


    @Override
    public String toString() {
        return """
                Flight{id=%d,
                flightNumber='%s',
                departureAirport='%s',
                arrivalAirport='%s',
                departureTime=%s,
                arrivalTime=%s}""".formatted(id, flightNumber, departureAirport, arrivalAirport, departureTime, arrivalTime);
    }
}

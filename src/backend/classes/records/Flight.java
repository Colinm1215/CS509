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
    private final String airline;
    private int seatsFree;
    private FlightInterface nextFlight;

    public Flight(int id, String flightNumber, String departureAirport, String arrivalAirport, Timestamp departureTime, Timestamp arrivalTime, String airline) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.airline = airline;
        this.seatsFree = 100;
        this.nextFlight = null;
    }

    public Flight(ResultSet rs) {
        try {
            this.id = rs.getInt("Id");
            this.flightNumber = rs.getString("FlightNumber");
            this.departureAirport = rs.getString("DepartAirport");
            this.arrivalAirport = rs.getString("ArriveAirport");
            this.departureTime = rs.getTimestamp("DepartDateTime");
            this.arrivalTime = rs.getTimestamp("ArriveDateTime");
            this.airline          = rs.getString("airline");
            this.seatsFree       = rs.getInt("SeatsAvailable");
            this.nextFlight = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Flight(FlightInterface flight, FlightInterface nextFlight) {
        try {
            this.id = flight.getId();
            this.flightNumber = flight.getFlightNumber();
            this.departureAirport = flight.getDepartureAirport();
            this.arrivalAirport = flight.getArrivalAirport();
            this.departureTime = flight.getDepartureTime();
            this.arrivalTime = flight.getArrivalTime();
            this.airline          = flight.getAirline();
            this.seatsFree       = flight.getSeatsFree();
            this.nextFlight = nextFlight;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getFlightNumber() {
        return flightNumber;
    }

    @Override
    public String getDepartureAirport() {
        return departureAirport;
    }

    @Override
    public String getArrivalAirport() {
        return arrivalAirport;
    }

    @Override
    public Timestamp getDepartureTime() {
        return departureTime;
    }

    @Override
    public Timestamp getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public int getSeatsFree() {
        return seatsFree;
    }

    @Override
    public String getAirline() {
        return airline.substring(0, 1).toUpperCase() + airline.substring(1, airline.length() - 1);

    }

    @Override
    public void reserved() {
        this.seatsFree = this.seatsFree - 1;
    }

    @Override
    public FlightInterface getNextFlight() {
        return this.nextFlight;
    }

    @Override
    public String toString() {
        return """
                Flight{id=%d,
                flightNumber='%s',
                departureAirport='%s',
                arrivalAirport='%s',
                departureTime=%s,
                arrivalTime=%s,
                airline=%s,
                seatsFree=%d}""".formatted(id, flightNumber, departureAirport, arrivalAirport, departureTime, arrivalTime, getAirline(), this.seatsFree);
    }
}

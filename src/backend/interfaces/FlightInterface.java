package backend.interfaces;

import java.sql.Timestamp;

public interface FlightInterface {
    int getId();
    String getFlightNumber();
    String getDepartureAirport();
    String getArrivalAirport();
    Timestamp getDepartureTime();
    Timestamp getArrivalTime();
    String getAirline();
}

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Flight
{
    private final int id;
    private final String flightNumber;
    private final String departureAirport;
    private final String arrivalAirport;
    private final Timestamp departureTime;
    private final Timestamp arrivalTime;

    public Flight(int id, String flightNumber, String departureAirport, String arrivalAirport,
                  Timestamp departureTime, Timestamp arrivalTime, String airline,
                  int availableSeats, BigDecimal price)
    {
        this.id = id;
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public int getId()
    {
        return id;
    }

    public String getFlightNumber()
    {
        return flightNumber;
    }

    public String getDepartureAirport()
    {
        return departureAirport;
    }

    public String getArrivalAirport()
    {
        return arrivalAirport;
    }

    public Timestamp getDepartureTime()
    {
        return departureTime;
    }

    public Timestamp getArrivalTime()
    {
        return arrivalTime;
    }


    @Override
    public String toString()
    {
        return "Flight{" +
                "id=" + id +
                ", flightNumber='" + flightNumber + '\'' +
                ", departureAirport='" + departureAirport + '\'' +
                ", arrivalAirport='" + arrivalAirport + '\'' +
                ", departureTime=" + departureTime +
                ", arrivalTime=" + arrivalTime +
                '}';
    }
}

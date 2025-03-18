import java.sql.*;
import java.util.List;

public class SearchService
{
    public static void main(String[] args)
    {// need to update with database / user input values
        try (Connection connection = DriverManager.getConnection("", "user", "password"))
        {
            FlightRepository flightRepo = new FlightRepository(connection);
            Timestamp startTime = Timestamp.valueOf("2025-04-01 00:00:00"); // from GUI
            Timestamp endTime = Timestamp.valueOf("2025-04-02 23:59:59"); // from GUI
            String departure = "JFK"; // from GUI
            String arrival = "JFK"; // from GUI
            String sort = "traveltime"; // from GUI
            List<Flight> flights = flightRepo.searchFlights(departure, arrival, startTime, endTime, sort);
            flights.forEach(System.out::println);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}

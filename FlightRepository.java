import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FlightRepository
{
    private final Connection connection;

    public FlightRepository(Connection connection)
    {
        this.connection = connection;
    }

    public List<Flight> searchFlights(String departureAirport, String arrivalAirport, Timestamp startTime, Timestamp endTime, String sortBy)
    {
        List<Flight> flights = new ArrayList<>();
        String orderByClause = switch (sortBy.toLowerCase())
        {
            case "departdatetime" -> "ORDER BY DepartDateTime ASC";
            case "arrivedatetime" -> "ORDER BY ArriveDateTime ASC";
            case "traveltime" -> "ORDER BY TIMESTAMPDIFF(MINUTE, DepartDateTime, ArriveDateTime) ASC";
            default -> "ORDER BY DepartDateTime ASC";
        };
        String query = """
            SELECT * FROM D1
            WHERE DepartAirport LIKE ? 
            AND ArriveAirport LIKE ? 
            AND DepartDateTime BETWEEN ? AND ? 
            """ + orderByClause;

        try (PreparedStatement stmt = connection.prepareStatement(query))
        {
            // Set parameters for the query
            stmt.setString(1, "%" + departureAirport + "%");
            stmt.setString(2, "%" + arrivalAirport + "%");
            stmt.setTimestamp(3, startTime);
            stmt.setTimestamp(4, endTime);

            ResultSet rs = stmt.executeQuery();
            while (rs.next())
            {
                Flight flight = new Flight(
                        rs.getInt("Id"),
                        rs.getString("FlightNumber"),
                        rs.getString("DepartAirport"),
                        rs.getString("ArriveAirport"),
                        rs.getTimestamp("DepartDateTime"),
                        rs.getTimestamp("ArriveDateTime")
                );
                flights.add(flight);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return flights;
    }
}

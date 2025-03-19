package backend.classes.services;

import backend.classes.database.Database;
import backend.classes.records.Flight;
import backend.interfaces.FlightInterface;
import enums.AirlineTable;
import backend.interfaces.DatabaseInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Service
public class FlightService {
    private DatabaseInterface database;

    @Value("${database.url}")
    private String dbUrl;
    @Value("${database.user}")
    private String dbUser;
    @Value("${database.password}")
    private String dbPassword;

    @PostConstruct
    public void init() throws SQLException {
        // Instantiate Database.java using parameters from application.properties.
        database = new Database(dbUrl, dbUser, dbPassword);
    }

    @PreDestroy
    public void close() throws SQLException {
        if (database != null) {
            database.close();
        }
    }

    // Search flights with pagination
    public Map<String, Object> searchFlights(String departureAirport, String arriveAirport, String startTimeStr,
                                             String endTimeStr, String sortBy, int page, int pageSize) throws SQLException {
        // Convert strings to Timestamps
        System.out.println(startTimeStr);
        String normalizedStart = startTimeStr.concat(" 00:00:00");
        Timestamp startTime = Timestamp.valueOf(normalizedStart);
        String normalizedEnd = endTimeStr.concat(" 23:59:59");
        Timestamp endTime = Timestamp.valueOf(normalizedEnd);

        // Prepare query parameters
        List<Object> params = new ArrayList<>();
        params.add(departureAirport);
        params.add(arriveAirport);
        params.add(startTime);
        params.add(endTime);

        // In this example, we query from two airline tables as in SearchService.java logic.
        List<AirlineTable> airlines = new ArrayList<>();
        airlines.add(AirlineTable.DELTAS);
        airlines.add(AirlineTable.SOUTHWESTS);

        List<FlightInterface> flights = database.selectFlights(airlines, sortBy, params);

        // Simple pagination logic
        int total = flights.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<FlightInterface> paginatedFlights = fromIndex < total ? flights.subList(fromIndex, toIndex) : new ArrayList<>();

        boolean hasMore = toIndex < total;
        Map<String, Object> response = new HashMap<>();
        response.put("flights", paginatedFlights);
        response.put("hasMore", hasMore);
        response.put("total", total);
        return response;
    }

    // Add a new flight
    public int addFlight(Flight flight) throws SQLException {
        List<Object> params = Arrays.asList(
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDepartureAirport(),
                flight.getArrivalAirport(),
                flight.getFlightNumber()
        );
        // Here, we assume DELTAS as default; in a real scenario, you might choose table dynamically.
        return database.insertFlight(AirlineTable.DELTAS, params);
    }

    // Update flight details
    public boolean updateFlight(int id, Flight flight) throws SQLException {
        // Params: [DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, id]
        List<Object> params = Arrays.asList(
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDepartureAirport(),
                flight.getArrivalAirport(),
                flight.getFlightNumber(),
                id
        );
        // Try to update in both tables for demonstration purposes.
        List<AirlineTable> tables = Arrays.asList(AirlineTable.DELTAS, AirlineTable.SOUTHWESTS);
        List<Integer> updatedIds = database.updateTables(tables, params);
        return !updatedIds.isEmpty();
    }

    // Delete flight by id
    public boolean deleteFlight(int id) throws SQLException {
        List<Object> params = Collections.singletonList(id);
        List<AirlineTable> tables = Arrays.asList(AirlineTable.DELTAS, AirlineTable.SOUTHWESTS);
        List<Integer> deletedIds = database.updateTables(tables, params);
        return !deletedIds.isEmpty();
    }
}

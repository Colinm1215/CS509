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
        database = new Database(dbUrl, dbUser, dbPassword);
    }

    @PreDestroy
    public void close() throws SQLException {
        if (database != null) {
            database.close();
        }
    }

    // Search flights
    public Map<String, Object> searchFlights(String departureAirport, String arriveAirport, String startTimeStr,
                                             String endTimeStr, String sortBy, int page, int pageSize) throws SQLException {
        System.out.println(startTimeStr);
        String normalizedStart = startTimeStr.concat(" 00:00:00");
        Timestamp startTime = Timestamp.valueOf(normalizedStart);
        String normalizedEnd = endTimeStr.concat(" 23:59:59");
        Timestamp endTime = Timestamp.valueOf(normalizedEnd);

        List<Object> params = new ArrayList<>();
        params.add(departureAirport);
        params.add(arriveAirport);
        params.add(startTime);
        params.add(endTime);

        List<AirlineTable> airlines = new ArrayList<>();
        airlines.add(AirlineTable.DELTAS);
        airlines.add(AirlineTable.SOUTHWESTS);

        List<FlightInterface> flights = database.selectFlights(airlines, sortBy, params);

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

    // Add flight
    public int addFlight(Flight flight, AirlineTable table) throws SQLException {
        List<Object> params = Arrays.asList(
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDepartureAirport(),
                flight.getArrivalAirport()
        );
        return database.insertFlight(table, params);
    }

    // Update flight
    public boolean updateFlight(int id, Flight flight) throws SQLException {
        // [DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, id]
        List<Object> params = Arrays.asList(
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getDepartureAirport(),
                flight.getArrivalAirport(),
                flight.getFlightNumber(),
                id
        );
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

package backend.classes.services;

import backend.classes.database.Database;
import backend.classes.records.Flight;
import backend.exceptions.NoSeatsAvailableException;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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


    public Map<String, Object> searchRoundTrip(
            String departureAirport,
            String arriveAirport,
            String startTimeStr,
            String endTimeStr,
            Integer maxStops,
            String airline,
            String returnDateStart,
            String returnDateEnd,
            String sortBy,
            int page,
            int pageSize
    ) throws SQLException {

        System.out.println(startTimeStr);
        List<AirlineTable> tables = new ArrayList<>();
        tables.add(AirlineTable.DELTAS);
        tables.add(AirlineTable.SOUTHWESTS);

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime startLocal = startTimeStr.isEmpty()
                ? database.getFlightWithEarliestDeparture(tables).getDepartureTime().toLocalDateTime()
                : LocalDateTime.parse(startTimeStr, isoFormatter);
        LocalDateTime endLocal = endTimeStr.isEmpty()
                ? database.getFlightWithLatestDeparture(tables).getDepartureTime().toLocalDateTime()
                : LocalDateTime.parse(endTimeStr,   isoFormatter);

        LocalDateTime returnDateStartLocal = returnDateStart.isEmpty()
                ? database.getFlightWithEarliestDeparture(tables).getDepartureTime().toLocalDateTime()
                : LocalDateTime.parse(returnDateStart, isoFormatter);
        LocalDateTime returnDateEndLocal = returnDateEnd.isEmpty()
                ? database.getFlightWithLatestDeparture(tables).getDepartureTime().toLocalDateTime()
                : LocalDateTime.parse(returnDateEnd,   isoFormatter);

        Timestamp startTime       = Timestamp.valueOf(startLocal);
        Timestamp endTime         = Timestamp.valueOf(endLocal);
        Timestamp returnDateStartTime = Timestamp.valueOf(returnDateStartLocal);
        Timestamp returnDateEndTime = Timestamp.valueOf(returnDateEndLocal);

        List<Object> params = List.of(
                departureAirport,
                arriveAirport,
                startTime,
                endTime,
                maxStops,
                airline,
                returnDateStartTime,
                returnDateEndTime
        );

        List<FlightInterface> flights = database.selectRoundTrip(tables, sortBy, params);

        int total = flights.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<FlightInterface> paginatedFlights = fromIndex < total ? flights.subList(fromIndex, toIndex) : new ArrayList<>();

        boolean hasMore = toIndex < total;
        System.out.println("Total Flights: " + total);
        System.out.println("Returning Flights: " + paginatedFlights.size());
        System.out.println("Has More Pages: " + hasMore);
        Map<String, Object> response = new HashMap<>();
        response.put("flights", paginatedFlights);
        response.put("hasMore", hasMore);
        response.put("total", total);
        return response;
    }


    // Search flights
    public Map<String, Object> searchFlights(
            String departureAirport,
            String arriveAirport,
            String startTimeStr,
            String endTimeStr,
            Integer maxStops,
            String airline,
            String sortBy,
            int page,
            int pageSize
    ) throws SQLException {
        System.out.println("Search Flights in Service");
        System.out.printf("departureAirport = '%s'%n", departureAirport);
        System.out.printf("arriveAirport    = '%s'%n", arriveAirport);
        System.out.printf("startTimeStr     = '%s'%n", startTimeStr);
        System.out.printf("endTimeStr       = '%s'%n", endTimeStr);
        System.out.printf("maxStops         = '%d'%n", maxStops);
        System.out.printf("airline          = '%s'%n", airline);
        System.out.printf("sortBy           = '%s'%n", sortBy);
        System.out.printf("page             = '%d'%n", page);
        System.out.printf("pageSize         = '%d'%n", pageSize);

        List<AirlineTable> tables = new ArrayList<>();
        tables.add(AirlineTable.DELTAS);
        tables.add(AirlineTable.SOUTHWESTS);

        LocalDateTime startLocalDateTime;
        LocalDateTime endLocalDateTime;

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;
        if (startTimeStr.isEmpty()) {
            System.out.println("Getting earliest departure time");
            FlightInterface earliestFlight = database.getFlightWithEarliestDeparture(tables);
            startLocalDateTime = earliestFlight.getDepartureTime().toLocalDateTime();
            System.out.println("Earliest departure time: " + startLocalDateTime);
        } else {
            startLocalDateTime = LocalDateTime.parse(startTimeStr, isoFormatter);
        }

        if (endTimeStr.isEmpty()) {
            System.out.println("Getting latest departure time");
            FlightInterface latestFlight = database.getFlightWithLatestDeparture(tables);
            endLocalDateTime = latestFlight.getDepartureTime().toLocalDateTime();
            System.out.println("Latest departure time: " + endLocalDateTime);
        } else {
            endLocalDateTime = LocalDateTime.parse(endTimeStr, isoFormatter);
        }

        Timestamp startTime = Timestamp.valueOf(startLocalDateTime);
        Timestamp endTime = Timestamp.valueOf(endLocalDateTime);

        List<Object> params = List.of(
                departureAirport,
                arriveAirport,
                startTime,
                endTime,
                maxStops,
                airline
        );

        System.out.println("Params are " + params);

        List<FlightInterface> flights = database.selectFlights(tables, sortBy, params);

        int total = flights.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<FlightInterface> paginatedFlights = fromIndex < total ? flights.subList(fromIndex, toIndex) : new ArrayList<>();

        boolean hasMore = toIndex < total;
        System.out.println("Total Flights: " + total);
        System.out.println("Returning Flights: " + paginatedFlights.size());
        System.out.println("Has More Pages: " + hasMore);
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

    // Select flight by id
    public FlightInterface getFlightById(int id) throws SQLException {
        List<AirlineTable> tables = List.of(AirlineTable.DELTAS, AirlineTable.SOUTHWESTS);
        FlightInterface flight = database.selectFlightById(tables, id);
        if (flight == null) {
            throw new NoSuchElementException("Flight not found: " + id);
        }
        return flight;
    }

    public FlightInterface decreaseSeatsAvailable(int id) throws SQLException {
        System.out.println("Reached decreaseSeatsAvailable in FlightService");
        String airline = this.getFlightById(id).getAirline().toLowerCase() + "s";
        AirlineTable table = switch (airline) {
            case "deltas" -> AirlineTable.DELTAS;
            case "southwests" -> AirlineTable.SOUTHWESTS;
            default -> throw new IllegalArgumentException("Invalid airline: " + airline);
        };
        if (!database.decreaseSeatsAvailable(table, id)) {
            System.out.println("No seats available for flight: " + id);
            throw new NoSeatsAvailableException(id);
        }
        FlightInterface flight = database.selectFlightById(List.of(AirlineTable.DELTAS, AirlineTable.SOUTHWESTS), id);
        if (flight == null) {
            throw new NoSuchElementException("Flight not found: " + id);
        }
        return flight;
    }
}

package backend.interfaces;

import enums.AirlineTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface DatabaseInterface extends AutoCloseable {

    ArrayList<FlightInterface> selectFlights(List<AirlineTable> tables,
                                             String sortBy,
                                             List<Object> params) throws SQLException;

    int insertFlight(AirlineTable table, List<Object> params) throws SQLException;

    ArrayList<Integer> updateTables(List<AirlineTable> tables, List<Object> params) throws SQLException;
    boolean decreaseSeatsAvailable(AirlineTable table, int flightId) throws SQLException;

        @Override
    void close() throws SQLException;

    FlightInterface getFlightWithEarliestDeparture(List<AirlineTable> tables) throws SQLException;

    FlightInterface getFlightWithLatestDeparture(List<AirlineTable> tables) throws SQLException;

    FlightInterface selectFlightById(List<AirlineTable> tables, int id) throws SQLException;

    ArrayList<FlightInterface> selectRoundTrip(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException;

}

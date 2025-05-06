package backend.classes.database;

import backend.classes.records.Flight;
import backend.interfaces.DatabaseInterface;
import backend.interfaces.FlightInterface;
import enums.AirlineTable;

import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Database implements DatabaseInterface {

    private final Connection connection;

    public Database(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
    }

    public int insertFlight(AirlineTable table, List<Object> params) throws SQLException {
        String sql = "INSERT INTO " + table.getTableName() +
                " (DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < 5; i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private List<FlightInterface> getAllFlightsFromTables(List<AirlineTable> tables, List<Object> params) throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tables.size(); i++)
        {
            sb.append("SELECT id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber ")
                    .append("FROM ").append(tables.get(i).getTableName()).append(" ")
                    .append("WHERE DepartDateTime BETWEEN ? AND ? ")
                    .append("AND DepartDateTime < ArriveDateTime ");

            if (i < tables.size() - 1)
            {
                sb.append(" UNION ");
            }
        }

        List<FlightInterface> flights = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sb.toString()))
        {
            int paramIndex = 1;
            for (int i = 0; i < tables.size(); i++)
            {
                pstmt.setObject(paramIndex++, params.get(2));
                pstmt.setObject(paramIndex++, params.get(3));
            }

            try (ResultSet rs = pstmt.executeQuery())
            {
                while (rs.next())
                {
                    Timestamp depart = rs.getTimestamp("DepartDateTime");
                    Timestamp arrive = rs.getTimestamp("ArriveDateTime");

                    if (arrive.after(depart))
                    {
                        flights.add(new Flight(rs));
                    }
                }
            }
        }

        return flights;
    }

    @Override
    public ArrayList<ArrayList<FlightInterface>> selectRoundTrip(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException {
        List<Object> firstParams = new ArrayList<>();
        List<Object> secondParams = new ArrayList<>();
        firstParams.add(params.get(0));
        firstParams.add(params.get(1));
        firstParams.add(params.get(2));
        firstParams.add(params.get(3));
        secondParams.add(params.get(1));
        secondParams.add(params.get(0));
        secondParams.add(params.get(4));
        secondParams.add(params.get(5));
        ArrayList<FlightInterface> flightsTo = selectFlights(tables, sortBy, firstParams);
        ArrayList<FlightInterface> flightsReturned = selectFlights(tables, sortBy, secondParams);
        ArrayList<ArrayList<FlightInterface>> itineraries = new ArrayList<>();
        itineraries.add(flightsTo);
        itineraries.add(flightsReturned);
        return itineraries;
    }

    public ArrayList<FlightInterface> selectFlights(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException {
        System.out.println("In Database.java");
        System.out.println("Params are " + params);
        System.out.println("Tables are " + tables);
        System.out.println("Sort by is " + sortBy);


        int maxStops = (int) params.get(4);
        String airlinePref = ((String) params.get(5)).toLowerCase();

        List<AirlineTable> filteredTables = switch (airlinePref) {
            case "southwests" -> List.of(AirlineTable.SOUTHWESTS);
            case "deltas" -> List.of(AirlineTable.DELTAS);
            default -> tables;
        };

        StringBuilder sb = new StringBuilder();
        String orderByClause = switch (sortBy.toLowerCase()) {
            case "arrivedatetime" -> "ORDER BY ArriveDateTime ASC";
            case "traveltime" -> "ORDER BY TIMESTAMPDIFF(MINUTE, DepartDateTime, ArriveDateTime) ASC";
            default -> "ORDER BY DepartDateTime ASC";
        };

        for (int i = 0; i < filteredTables.size(); i++) {
            String tableName = filteredTables.get(i).getTableName();
            sb.append("SELECT id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, SeatsAvailable, '")
                    .append(tableName).append("' AS airline ")
                    .append(" FROM ").append(tableName)
                    .append(" WHERE DepartAirport LIKE ? ")
                    .append(" AND ArriveAirport LIKE ? ")
                    .append(" AND DepartDateTime BETWEEN ? AND ? ")
                    .append(" AND DepartDateTime < ArriveDateTime ");

            if (i < filteredTables.size() - 1) {
                sb.append(" UNION ");
            }
        }

        sb.append(" ").append(orderByClause);
        ArrayList<FlightInterface> flights = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            for (int i = 0; i < filteredTables.size(); i++) {
                pstmt.setObject(paramIndex++, "".equals(params.get(0)) ? "%" : "%" + params.get(0) + "%");
                pstmt.setObject(paramIndex++, "".equals(params.get(1)) ? "%" : "%" + params.get(1) + "%");
                pstmt.setObject(paramIndex++, params.get(2));
                pstmt.setObject(paramIndex++, params.get(3));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    flights.add(new Flight(rs));
                }
            }
        }

        if (!flights.isEmpty()) {
            System.out.println("Found direct flights: " + flights.size());
            return flights;
        }

        System.out.println("No direct flights found.");
        if (maxStops < 1) return flights;

        System.out.println("Searching for connecting flights...");
        List<FlightInterface> allFlights = getAllFlightsFromTables(tables, params);
        ArrayList<FlightInterface> connecting = new ArrayList<>();

        for (FlightInterface first : allFlights) {
            for (FlightInterface second : allFlights) {
                if (first.getDepartureAirport().contains(params.get(0).toString().toUpperCase())
                        && first.getArrivalAirport().equals(second.getDepartureAirport())
                        && second.getArrivalAirport().contains(params.get(1).toString().toUpperCase()))
                {

                    long layover = Duration.between(
                            first.getArrivalTime().toLocalDateTime(),
                            second.getDepartureTime().toLocalDateTime()
                    ).toMinutes();

                    if (layover >= 30 && layover < 360 && first.getArrivalTime().before(second.getDepartureTime()) && !first.equals(second)) {
                        connecting.add(first);
                        connecting.add(second);
                    }
                }
            }
        }

        if (!connecting.isEmpty()) {
            System.out.println("Found 1-stop flights: " + connecting.size() / 2 + " pairs");
            return connecting;
        }

        if (maxStops < 2) return connecting;

        System.out.println("Searching for 2-stop flights...");
        for (FlightInterface first : allFlights) {
            if (!first.getDepartureAirport().contains(params.get(0).toString().toUpperCase())) continue;

            for (FlightInterface second : allFlights) {
                if (!first.getArrivalAirport().equals(second.getDepartureAirport())) continue;

                for (FlightInterface third : allFlights) {
                    if (!second.getArrivalAirport().equals(third.getDepartureAirport())) continue;
                    if (!third.getArrivalAirport().contains(params.get(1).toString().toUpperCase())) continue;

                    long layover1 = Duration.between(first.getArrivalTime().toLocalDateTime(), second.getDepartureTime().toLocalDateTime()).toMinutes();
                    long layover2 = Duration.between(second.getArrivalTime().toLocalDateTime(), third.getDepartureTime().toLocalDateTime()).toMinutes();

                    if (layover1 >= 30 && layover2 >= 30 && layover2 < 360 && layover1 < 360 &&
                            first.getArrivalTime().before(second.getDepartureTime()) &&
                            second.getArrivalTime().before(third.getDepartureTime()) &&
                            !first.equals(second) && !second.equals(third) && !first.equals(third)) {
                        connecting.add(first);
                        connecting.add(second);
                        connecting.add(third);
                    }
                }
            }
        }

        if (!connecting.isEmpty()) {
            System.out.println("Found 2-stop flights: " + connecting.size() / 3 + " sets");
            return connecting;
        }

        System.out.println("No flight plans found with max " + maxStops + " stop(s).");
        return connecting;
    }


    public ArrayList<Integer> updateTables(List<AirlineTable> tables, List<Object> params) throws SQLException {
        ArrayList<Integer> ids = new ArrayList<>();

        for (AirlineTable table : tables) {
            String sql;
            if (params.size() == 1) {
                sql = "DELETE FROM " + table.getTableName() + " WHERE id = ?";
            } else {
                // Params MUST have
                // [DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, id]
                sql = "UPDATE " + table.getTableName() + " SET DepartDateTime = ?, ArriveDateTime = ?, " + "DepartAirport = ?, ArriveAirport = ?, FlightNumber = ? WHERE id = ?";
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }
            }

            String selectSql = "SELECT * FROM " + table.getTableName() + " WHERE id = ?";
            try (PreparedStatement st = connection.prepareStatement(selectSql)) {
                st.setObject(1, params.get(params.size() - 1));
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Updated Flight ID: " + rs.getInt("id"));
                        System.out.println("DepartDateTime: " + rs.getTimestamp("DepartDateTime"));
                        System.out.println("ArriveDateTime: " + rs.getTimestamp("ArriveDateTime"));
                        System.out.println("DepartAirport: " + rs.getString("DepartAirport"));
                        System.out.println("ArriveAirport: " + rs.getString("ArriveAirport"));
                        System.out.println("FlightNumber: " + rs.getString("FlightNumber"));
                        System.out.println("SeatsAvailable: " + rs.getInt("SeatsAvailable"));
                    }
                }
            }
        }


        return ids;
    }

    public boolean decreaseSeatsAvailable(AirlineTable table, int flightId) throws SQLException
    {
        System.out.println("reached decreaseSeatsAvailable in Database.java");
        ArrayList<Integer> ids = new ArrayList<>();
        System.out.println(table.getTableName());

        String sql = "UPDATE " + table.getTableName() +
                " SET SeatsAvailable = SeatsAvailable - 1 " +
                "WHERE id = ? AND SeatsAvailable > 0";

        System.out.println("SQL: " + sql);
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            stmt.setInt(1, flightId);  // Set the flight ID in the prepared statement
            System.out.println("stmt: " + stmt);
            int updated = stmt.executeUpdate();  // Execute the update statement
            System.out.println("Updated: " + updated);

            if (updated > 0)  // If the update was successful, proceed
            {
                return true;
            }
            else  // If no rows were updated (no seats available or invalid flight ID)
            {
                System.out.println("No seats available or invalid flight ID in table: " + table.getTableName());
                return false;
            }
        }
    }


    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    public FlightInterface getFlightWithEarliestDeparture(List<AirlineTable> tables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            String tableName = tables.get(i).getTableName();
            sb.append("SELECT ")
                    .append("id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, SeatsAvailable, ")
                    .append("'").append(tableName).append("' AS airline ")
                    .append("FROM ").append(tableName);
            if (i < tables.size() - 1) {
                sb.append(" UNION ALL ");
            }
        }
        sb.append(" ORDER BY DepartDateTime ASC LIMIT 1");
        String sql = sb.toString();;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Flight(rs);
                } else {
                    throw new SQLException("No flights found for tables: " + tables + " and query: " + sql);
                }
            }
        }
    }


    @Override
    public FlightInterface getFlightWithLatestDeparture(List<AirlineTable> tables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            String tableName = tables.get(i).getTableName();
            sb.append("SELECT ")
                    .append("id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, SeatsAvailable, ")
                    .append("'").append(tableName).append("' AS airline ")
                    .append("FROM ").append(tableName);
            if (i < tables.size() - 1) {
                sb.append(" UNION ALL ");
            }
        }
        sb.append(" ORDER BY DepartDateTime DESC LIMIT 1");

        try (PreparedStatement pstmt = connection.prepareStatement(sb.toString())) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Flight(rs);
                }
            }
        }
        return null;
    }


    public FlightInterface selectFlightById(List<AirlineTable> tables, int id) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            String tableName = tables.get(i).getTableName();
            sb.append("SELECT ")
                    .append("id, ")
                    .append("DepartDateTime, ")
                    .append("ArriveDateTime, ")
                    .append("DepartAirport, ")
                    .append("ArriveAirport, ")
                    .append("FlightNumber, ")
                    .append("SeatsAvailable, ")
                    .append("'").append(tableName).append("' AS airline ")
                    .append("FROM ").append(tableName)
                    .append(" WHERE id = ?");
            if (i < tables.size() - 1) {
                sb.append(" UNION ALL ");
            }
        }
        sb.append(" LIMIT 1");

        try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
            for (int idx = 1; idx <= tables.size(); idx++) {
                stmt.setInt(idx, id);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Flight(rs);
                }
            }
        }
        return null;
    }
}

package backend.classes.database;

import backend.classes.records.Flight;
import backend.interfaces.DatabaseInterface;
import backend.interfaces.FlightInterface;
import enums.AirlineTable;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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
            sb.append("SELECT id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, SeatsAvailable, '")
                    .append(tables.get(i).getTableName()).append("' AS airline ")
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
    public ArrayList<FlightInterface> selectRoundTrip(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException {
        List<Object> firstParams = new ArrayList<>();
        List<Object> secondParams = new ArrayList<>();

        System.out.println("In round trip");
        System.out.println("Params are " + params);
        System.out.println("Tables are " + tables);
        System.out.println("Sort by is " + sortBy);

        // outbound
        firstParams.add(params.get(0)); // depart
        firstParams.add(params.get(1)); // arrive
        firstParams.add(params.get(2)); // outbound window start
        firstParams.add(params.get(3)); // outbound window end
        firstParams.add(params.get(4)); // maxStops
        firstParams.add(params.get(5)); // airlinePref

        // return
        secondParams.add(params.get(1)); // depart = original arrive
        secondParams.add(params.get(0)); // arrive = original depart
        secondParams.add(params.get(6)); // return window start
        secondParams.add(params.get(7)); // return window end
        secondParams.add(params.get(4)); // maxStops
        secondParams.add(params.get(5)); // airlinePref

        ArrayList<FlightInterface> flightsTo = selectFlights(tables, sortBy, firstParams);
        ArrayList<FlightInterface> flightsReturned = selectFlights(tables, sortBy, secondParams);

        ArrayList<FlightInterface> roundTrips = new ArrayList<>();

        int n = Math.min(flightsTo.size(), flightsReturned.size());
        for (int i = 0; i < n; i++) {
            FlightInterface inOrig = flightsReturned.get(i);
            List<FlightInterface> inLegs = new ArrayList<>();
            while (inOrig != null) {
                inLegs.add(inOrig);
                inOrig = inOrig.getNextFlight();
            }
            FlightInterface inHeadCopy = null;
            for (int j = inLegs.size() - 1; j >= 0; j--) {
                inHeadCopy = new Flight(inLegs.get(j), inHeadCopy);
            }

            FlightInterface outOrig = flightsTo.get(i);
            List<FlightInterface> outLegs = new ArrayList<>();
            while (outOrig != null) {
                outLegs.add(outOrig);
                outOrig = outOrig.getNextFlight();
            }

            FlightInterface chain = null;
            for (int j = outLegs.size() - 1; j >= 0; j--) {
                if (j == 0) {
                    chain = new Flight(outLegs.get(0), chain, inHeadCopy);
                } else {
                    chain = new Flight(outLegs.get(j), chain);
                }
            }

            roundTrips.add(chain);
        }

        return roundTrips;
    }

    private void dfsConnections(String target,
                                List<FlightInterface> path,
                                List<List<FlightInterface>> results,
                                Map<String, NavigableMap<LocalDateTime, List<FlightInterface>>> departuresByTime,
                                int maxStops) {
        FlightInterface last = path.get(path.size() - 1);
        String lastCode = code(last.getArrivalAirport()).toUpperCase();
        LocalDateTime arr = last.getArrivalTime().toLocalDateTime();

        if (code(last.getArrivalAirport()).equals(target)) {
            results.add(new ArrayList<>(path));
            return;
        }
        if (path.size() > maxStops + 1) return;

        LocalDateTime windowStart = arr.plusMinutes(30);
        LocalDateTime windowEnd   = arr.plusMinutes(359);

        NavigableMap<LocalDateTime, List<FlightInterface>> tm =
                departuresByTime.getOrDefault(lastCode, new TreeMap<>());

        for (var entry : tm.subMap(windowStart, true, windowEnd, true).entrySet()) {
            for (FlightInterface next : entry.getValue()) {
                if (path.contains(next)) continue;

                path.add(next);
                dfsConnections(target, path, results, departuresByTime, maxStops);
                path.remove(path.size() - 1);
            }
        }
    }

    private String code(String airportWithParen) {
        int p0 = airportWithParen.indexOf('(');
        int p1 = airportWithParen.indexOf(')');
        if (p0 >= 0 && p1 > p0) {
            return airportWithParen.substring(p0 + 1, p1);
        }
        return airportWithParen.length() >= 3
                ? airportWithParen.substring(airportWithParen.length() - 3)
                : airportWithParen;
    }

    private List<Object> ensureFullDayRange(List<Object> params) {
        params = new ArrayList<>(params);
        if (params.get(2) instanceof Timestamp) {
            Timestamp start = (Timestamp) params.get(2);
            LocalDateTime startOfDay = start.toLocalDateTime().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            params.set(2, Timestamp.valueOf(startOfDay));
            params.set(3, Timestamp.valueOf(endOfDay));
        }
        return params;
    }

    public ArrayList<FlightInterface> selectFlights(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException {
        System.out.println("In Database.java");
        System.out.println("Params are " + params);
        System.out.println("Tables are " + tables);
        System.out.println("Sort by is " + sortBy);
        params = ensureFullDayRange(params);

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

        Map<String, NavigableMap<LocalDateTime, List<FlightInterface>>> departuresByTime = new HashMap<>();

        for (FlightInterface f : allFlights) {
            String depCode = code(f.getDepartureAirport()).toUpperCase();
            LocalDateTime depTime = f.getDepartureTime().toLocalDateTime();

            departuresByTime
                    .computeIfAbsent(depCode, k -> new TreeMap<>())
                    .computeIfAbsent(depTime,   k -> new ArrayList<>())
                    .add(f);
        }

        Map<String, List<FlightInterface>> flightsFrom = new HashMap<>();
        for (FlightInterface f : allFlights) {
            String depCode = code(f.getDepartureAirport()).toUpperCase();
            flightsFrom
                    .computeIfAbsent(depCode, k -> new ArrayList<>())
                    .add(f);
        }

        String origin = params.get(0).toString().toUpperCase();
        String dest   = params.get(1).toString().toUpperCase();

        System.out.println("origin key   : '" + origin + "'");
        System.out.println("available keys: " + flightsFrom.keySet());

        ArrayList<FlightInterface> connecting = new ArrayList<>();
        List<List<FlightInterface>> raw = new ArrayList<>();

        for (FlightInterface firstLeg : flightsFrom.getOrDefault(origin, List.of())) {
            List<FlightInterface> path = new ArrayList<>();
            path.add(firstLeg);

            dfsConnections(dest, path, raw, departuresByTime, maxStops);
        }

        System.out.println("Found " + raw.size() + " possible connections.");
        System.out.println("raw is " + raw);

        for (List<FlightInterface> legs : raw) {
            FlightInterface composite = legs.get(0);
            for (int i = 1; i < legs.size(); i++) {
                composite = new Flight(composite, legs.get(i));
            }
            connecting.add(composite);
        }

        System.out.println("Found " + connecting.size() + " connecting flights.");
        System.out.println("Connecting flights are " + connecting);

        if (!connecting.isEmpty()) {
            System.out.println("Found connecting flights: " + connecting.size());
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

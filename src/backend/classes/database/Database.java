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

    private List<FlightInterface> getAllFlightsFromTables(List<AirlineTable> tables, List<Object> params) throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tables.size(); i++)
        {
            sb.append("SELECT id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber ")
                    .append("FROM ").append(tables.get(i).getTableName())
                    .append(" WHERE DepartDateTime BETWEEN ? AND ?")
                    .append(" AND DepartDateTime < ArriveDateTime");

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

    public ArrayList<FlightInterface> selectFlights(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String orderByClause = switch (sortBy.toLowerCase()) {
            case "arrivedatetime" -> "ORDER BY ArriveDateTime ASC";
            case "traveltime" -> "ORDER BY TIMESTAMPDIFF(MINUTE, DepartDateTime, ArriveDateTime) ASC";
            default -> "ORDER BY DepartDateTime ASC";
        };

        for (int i = 0; i < tables.size(); i++) {
            String tableName = tables.get(i).getTableName();
            sb.append("SELECT id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, '")
                    .append(tableName).append("' AS airline ")
                    .append("FROM ").append(tableName)
                    .append(" WHERE DepartAirport LIKE ? ")
                    .append("AND ArriveAirport LIKE ? ")
                    .append("AND DepartDateTime BETWEEN ? AND ?")
                    .append(" AND DepartDateTime < ArriveDateTime");
            if (i < tables.size() - 1) {
                sb.append(" UNION ");
            }
        }
        sb.append(" ").append(orderByClause);

        ArrayList<FlightInterface> flights = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            for (int i = 0; i < tables.size(); i++) {
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
            System.out.println("Found direct flights: " + flights.size());
        }

        if (!flights.isEmpty())
        {
            System.out.println("Found direct flights: " + flights.size());
            return flights;
        }

        System.out.println("No direct flights found. Searching for connecting flights...");
        // Try to find connecting flights
        List<FlightInterface> allFlights = getAllFlightsFromTables(tables, params);
        ArrayList<FlightInterface> connecting = new ArrayList<>();
        for (FlightInterface firstLeg : allFlights)
        {
            for (FlightInterface secondLeg : allFlights)
            {
                if (firstLeg.getDepartureAirport().contains((CharSequence) params.get(0)) && firstLeg.getArrivalAirport().equals(secondLeg.getDepartureAirport()) && secondLeg.getArrivalAirport().contains((CharSequence) params.get(1)))
                {
                    long layoverMinutes = Duration.between(
                            firstLeg.getArrivalTime().toLocalDateTime(),
                            secondLeg.getDepartureTime().toLocalDateTime()
                    ).toMinutes();

                    if (layoverMinutes >= 30 &&
                            firstLeg.getArrivalTime().before(secondLeg.getDepartureTime()) &&
                            !firstLeg.equals(secondLeg))
                    {
                        connecting.add(firstLeg);
                        connecting.add(secondLeg);
                    }
                }
            }
        }

        if (!connecting.isEmpty())
        {
            System.out.println("Found connecting flights: " + connecting.size() / 2 + " pairs");
            return connecting;
        }

        System.out.println("No direct or single connection flights found. Searching for double connecting flights...");
        for (FlightInterface firstLeg : allFlights)
        {
            if(!firstLeg.getDepartureAirport().contains((CharSequence) params.get(0)))
            {
                continue;
            }
            for (FlightInterface secondLeg : allFlights)
            {
                if(!firstLeg.getArrivalAirport().equals(secondLeg.getDepartureAirport())){
                    continue;
                }
                for (FlightInterface thirdLeg : allFlights)
                {
                    if (!thirdLeg.getArrivalAirport().contains((CharSequence) params.get(1)) ||
                            !secondLeg.getArrivalAirport().equals(thirdLeg.getDepartureAirport())
                    )
                    {
                        continue;
                    }
                    {
                        long layover1 = Duration.between(
                                firstLeg.getArrivalTime().toLocalDateTime(),
                                secondLeg.getDepartureTime().toLocalDateTime()
                        ).toMinutes();

                        long layover2 = Duration.between(
                                secondLeg.getArrivalTime().toLocalDateTime(),
                                thirdLeg.getDepartureTime().toLocalDateTime()
                        ).toMinutes();

                        if (layover1 >= 30 &&
                                layover2 >= 30 &&
                                firstLeg.getArrivalTime().before(secondLeg.getDepartureTime()) &&
                                secondLeg.getArrivalTime().before(thirdLeg.getDepartureTime()) &&
                                !firstLeg.equals(secondLeg) &&
                                !secondLeg.equals(thirdLeg) &&
                                !firstLeg.equals(thirdLeg))
                        {
                            connecting.add(firstLeg);
                            connecting.add(secondLeg);
                            connecting.add(thirdLeg);
                        }
                    }
                }
            }
        }
        if (!connecting.isEmpty()){
            System.out.println("Found triple connecting flights: " + connecting.size() / 3 + " sets");
            return connecting;
        }
        System.out.println("No flight plans with <3 connections found");
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
        }
        return ids;
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
                    .append("id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, ")
                    .append("'").append(tableName).append("' AS airline ")
                    .append("FROM ").append(tableName);
            if (i < tables.size() - 1) {
                sb.append(" UNION ALL ");
            }
        }
        sb.append(" ORDER BY DepartDateTime ASC LIMIT 1");

        try (PreparedStatement pstmt = connection.prepareStatement(sb.toString())) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Flight(rs);
                }
            }
        }
        return null;
    }


    @Override
    public FlightInterface getFlightWithLatestDeparture(List<AirlineTable> tables) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            String tableName = tables.get(i).getTableName();
            sb.append("SELECT ")
                    .append("id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber, ")
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

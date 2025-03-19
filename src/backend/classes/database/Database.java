package backend.classes.database;

import backend.classes.records.Flight;
import backend.interfaces.DatabaseInterface;
import backend.interfaces.FlightInterface;
import enums.AirlineTable;

import java.sql.*;
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


    public ArrayList<FlightInterface> selectFlights(List<AirlineTable> tables, String sortBy, List<Object> params) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String orderByClause = switch (sortBy.toLowerCase()) {
            case "arrivedatetime" -> "ORDER BY ArriveDateTime ASC";
            case "traveltime" -> "ORDER BY TIMESTAMPDIFF(MINUTE, DepartDateTime, ArriveDateTime) ASC";
            default -> "ORDER BY DepartDateTime ASC";
        };

        for (int i = 0; i < tables.size(); i++) {
            sb.append("SELECT id, DepartDateTime, ArriveDateTime, DepartAirport, ArriveAirport, FlightNumber ").append("FROM ").append(tables.get(i).getTableName()).append(" WHERE ").append("DepartAirport LIKE ? ").append("AND ArriveAirport LIKE ? ").append("AND DepartDateTime BETWEEN ? AND ?");

            if (i < tables.size() - 1) {
                sb.append(" UNION ");
            }
        }
        sb.append(orderByClause);
        ArrayList<FlightInterface> flights = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            for (int i = 0; i < tables.size(); i++) {
                pstmt.setObject(paramIndex++, "%" + params.get(0) + "%");
                pstmt.setObject(paramIndex++, "%" + params.get(1) + "%");
                pstmt.setObject(paramIndex++, params.get(2));
                pstmt.setObject(paramIndex++, params.get(3));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    flights.add(new Flight(rs));
                }
            }

            return flights;
        }
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
}

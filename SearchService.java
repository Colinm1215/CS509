import backend.classes.Flight;
import backend.classes.Database;
import backend.interfaces.DatabaseInterface;
import backend.interfaces.FlightInterface;
import enums.AirlineTable;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SearchService {
    public static void main(String[] args) {
        try (DatabaseInterface flightRepo = new Database("jdbc:mysql://127.0.0.1:3306/flightdata", "root", "root")) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = null;

        while (startDate == null) {
            System.out.print("Enter a date to depart (yyyy-MM-dd): ");
            String userInput = scanner.nextLine();

            try {
                startDate = LocalDate.parse(userInput, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
            }
        }

        System.out.println("Enter the three letter ID code for your departure airport");
        String departureAirport = scanner.nextLine().substring(0, 3).toUpperCase();
        System.out.println("Enter the three letter ID code for your arrival airport");
        String arriveAirport = scanner.nextLine().substring(0, 3).toUpperCase();
        String depart = startDate + " 00:00:00";
        String latest = startDate + " 23:59:59";
        Timestamp startTime = Timestamp.valueOf(depart);
        Timestamp endTime = Timestamp.valueOf(latest);
        String sort = "traveltime"; // from GUI
        List<Object> params = new ArrayList<>();
        params.add(departureAirport);
        params.add(arriveAirport);
        params.add(startTime);
        params.add(endTime);
        List<AirlineTable> airlines = new ArrayList<>();
        airlines.add(AirlineTable.DELTAS);
        airlines.add(AirlineTable.SOUTHWESTS);
        List<FlightInterface> flights = flightRepo.selectFlights(airlines, sort, params);
        flights.forEach(System.out::println);
        if (flights.isEmpty()) {
            System.out.println("No flights found.");
        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class SearchService
{
    public static void main(String[] args)
    {// need to update with database / user input values
        try (Connection connection = DriverManager.getConnection("", "user", "password"))
        {
            FlightRepository flightRepo = new FlightRepository(connection);
            Scanner scanner = new Scanner(System.in);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = null;

            while (startDate == null)
            {
                System.out.print("Enter a date to depart (yyyy-MM-dd): ");
                String userInput = scanner.nextLine();

                try
                {
                    startDate = LocalDate.parse(userInput, formatter);
                }
                catch (DateTimeParseException e)
                {
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
            List<Flight> flights = flightRepo.searchFlights(departureAirport, arriveAirport, startTime, endTime, sort);
            flights.forEach(System.out::println);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}

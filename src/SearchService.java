import backend.classes.database.Database;
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
    public static void main(String[] args)
    {
        try (DatabaseInterface flightRepo = new Database("jdbc:mysql://127.0.0.1:3306/jawn", "root", "JtcJtc123!"))
        {
            Scanner scanner = new Scanner(System.in);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            System.out.print("Search for (1) One-way or (2) Round-trip flights? Enter 1 or 2: ");
            String tripType = scanner.nextLine().trim();

            System.out.println("Enter the three letter ID code for your departure airport");
            String departureAirport = scanner.nextLine().substring(0, 3).toUpperCase();

            System.out.println("Enter the three letter ID code for your arrival airport");
            String arriveAirport = scanner.nextLine().substring(0, 3).toUpperCase();

            LocalDate startDate = null;
            while (startDate == null)
            {
                System.out.print("Enter a date to depart (yyyy-MM-dd): ");
                try
                {
                    startDate = LocalDate.parse(scanner.nextLine(), formatter);
                }
                catch (DateTimeParseException e)
                {
                    System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
                }
            }

            List<AirlineTable> airlines = new ArrayList<>();
            airlines.add(AirlineTable.DELTAS);
            airlines.add(AirlineTable.SOUTHWESTS);

            String sort = "traveltime"; // this could be customized from GUI

            String departFirst = startDate + " 00:00:00";
            String latestFirst = startDate + " 23:59:59";
            Timestamp startTime1 = Timestamp.valueOf(departFirst);
            Timestamp endTime1 = Timestamp.valueOf(latestFirst);
            List<Object> params = new ArrayList<>();
            params.add(departureAirport);
            params.add(arriveAirport);
            params.add(startTime1);
            params.add(endTime1);

            if (tripType.equals("2")) // Round-trip
            {
                LocalDate returnDate = null;
                while (returnDate == null)
                {
                    System.out.print("Enter a date to return (yyyy-MM-dd): ");
                    try
                    {
                        returnDate = LocalDate.parse(scanner.nextLine(), formatter);
                    }
                    catch (DateTimeParseException e)
                    {
                        System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
                    }
                }
                String departSecond = returnDate + " 00:00:00";
                String latestSecond = returnDate + " 23:59:59";
                Timestamp startTime2 = Timestamp.valueOf(departSecond);
                Timestamp endTime2 = Timestamp.valueOf(latestSecond);
                params.add(startTime2);
                params.add(endTime2);
                ArrayList<ArrayList<FlightInterface>> roundTrips = flightRepo.selectRoundTrip(
                        airlines, sort, params
                );

                List<FlightInterface> outbound = roundTrips.get(0);
                List<FlightInterface> returns = roundTrips.get(1);
                if (outbound.isEmpty()){
                    System.out.println("No outbound flight plans with <3 connections found.");
                }
                else if (returns.isEmpty()){
                    System.out.println("No return flight plans with <3 connections found.");
                }
                else {
                    System.out.println("Outbound flight options: ");
                    outbound.forEach(System.out::println);
                    System.out.println("Return flight options: ");
                    outbound.forEach(System.out::println);
                }
            }
            else // One-way
            {
                List<FlightInterface> flights = flightRepo.selectFlights(airlines, sort, params);
                flights.forEach(System.out::println);

                if (flights.isEmpty())
                {
                    System.out.println("No one-way flights found.");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

}

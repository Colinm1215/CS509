package backend.classes.controllers;

import backend.classes.records.Flight;
import backend.classes.services.FlightService;
import enums.AirlineTable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/flights")
@CrossOrigin(origins = "*")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    // flights?departureAirport=...&arriveAirport=...&startTime=...&endTime=...&sortBy=...&page=...&pageSize=...
    @GetMapping
    public ResponseEntity<Map<String, Object>> getFlights(
            @RequestParam String departureAirport,
            @RequestParam String arriveAirport,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(defaultValue = "traveltime") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int pageSize) {
        try {
            Map<String, Object> result = flightService.searchFlights(departureAirport, arriveAirport, startTime, endTime, sortBy, page, pageSize);
            System.out.println("Received request: page=" + page + ", pageSize=" + pageSize);
            return ResponseEntity.ok(result);
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    // Add Flight
    // Currently utilizing Deltas table as a placeholder
    @PostMapping
    public ResponseEntity<?> addFlight(@RequestBody Flight flight) {
        try {
            int newId = flightService.addFlight(flight, AirlineTable.DELTAS);
            return ResponseEntity.ok(Map.of("id", newId));
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    //  Update flight by ID
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFlight(@PathVariable int id, @RequestBody Flight flight) {
        try {
            boolean updated = flightService.updateFlight(id, flight);
            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Flight updated"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Flight not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    // Delete flight by id
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFlight(@PathVariable int id) {
        try {
            boolean deleted = flightService.deleteFlight(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Flight deleted"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Flight not found"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }
}

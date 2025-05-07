package backend.exceptions;

public class NoSeatsAvailableException extends RuntimeException {
    public NoSeatsAvailableException(int flightId) {
        super("No seats available for flight ID: " + flightId);
    }
}
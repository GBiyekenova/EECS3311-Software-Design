package finassist.exception;

/** Thrown when user input is missing or invalid (amount, date, required fields...). */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}

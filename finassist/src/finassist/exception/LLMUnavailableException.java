package finassist.exception;

/** Thrown when the external AI/LLM or Speech-to-Text service cannot be reached. */
public class LLMUnavailableException extends RuntimeException {
    public LLMUnavailableException(String message) { super(message); }
    public LLMUnavailableException(String message, Throwable cause) { super(message, cause); }
}

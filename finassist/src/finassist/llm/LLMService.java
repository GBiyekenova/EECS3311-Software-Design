package finassist.llm;

/** Gateway to the external AI/LLM. Throws LLMUnavailableException when the service cannot be reached. */
public interface LLMService {
    String generate(String prompt);
}

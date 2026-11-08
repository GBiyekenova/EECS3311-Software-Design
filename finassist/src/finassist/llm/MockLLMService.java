package finassist.llm;

/** Offline stand-in used when no OPENAI_API_KEY is set. It does not reason; it only echoes the computed facts. */
public class MockLLMService implements LLMService {
    private boolean available = true;

    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String generate(String prompt) {
        if (!available) throw new finassist.exception.LLMUnavailableException("mock LLM switched off");
        String[] lines = prompt.split("\n");
        StringBuilder facts = new StringBuilder();
        for (int i = Math.max(0, lines.length - 8); i < lines.length; i++) if (!lines[i].isBlank()) facts.append(lines[i]).append('\n');
        return "[Offline demo AI - set OPENAI_API_KEY for real answers]\n" + facts.toString().trim();
    }
}

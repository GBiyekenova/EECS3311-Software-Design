package finassist.llm;

import finassist.exception.LLMUnavailableException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Calls the OpenAI chat completions API. Needs the OPENAI_API_KEY environment variable. */
public class OpenAILLMService implements LLMService {
    private final String apiKey;
    private final String model;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public OpenAILLMService(String apiKey) {
        this(apiKey, System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini"));
    }

    public OpenAILLMService(String apiKey, String model) { this.apiKey = apiKey; this.model = model; }

    @Override
    public String generate(String prompt) {
        String body = "{\"model\":\"" + model + "\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"You are FinAssist, a careful personal finance assistant. "
                + "Use only the data you are given. If data is missing, say so instead of guessing. Be concise.\"},"
                + "{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new LLMUnavailableException("AI service returned HTTP " + response.statusCode());
            String text = extractJsonString(response.body(), "content");
            if (text == null || text.isBlank()) throw new LLMUnavailableException("AI service returned an empty answer");
            return text.trim();
        } catch (IOException e) {
            throw new LLMUnavailableException("AI service could not be reached", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMUnavailableException("AI request was interrupted", e);
        }
    }

    /** Quotes and escapes a Java string as a JSON string. */
    public static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** Returns the first JSON string value stored under the given key, or null. */
    public static String extractJsonString(String json, String key) {
        int k = json.indexOf("\"" + key + "\"");
        if (k < 0) return null;
        int colon = json.indexOf(':', k);
        int i = json.indexOf('"', colon + 1);
        if (colon < 0 || i < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (i = i + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return sb.toString();
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u': sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16)); i += 4; break;
                    default: sb.append(n);
                }
            } else sb.append(c);
        }
        return null;
    }
}

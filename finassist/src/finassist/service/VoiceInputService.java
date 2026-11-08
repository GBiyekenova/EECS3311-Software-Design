package finassist.service;

import finassist.exception.LLMUnavailableException;
import finassist.llm.OpenAILLMService;
import finassist.model.Expense;
import finassist.model.FinancialEntry;
import finassist.model.Income;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voice financial logging (F01).
 * captureSpeech(): records the microphone and sends the audio to the OpenAI speech-to-text model.
 * When that is not possible (no key / no microphone) it asks the fallback supplier, which lets the user type the sentence.
 */
public class VoiceInputService {
    private static final Pattern AMOUNT = Pattern.compile("\\$?(\\d+(?:[.,]\\d{1,2})?)");
    private static final Pattern DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern MERCHANT = Pattern.compile("\\b(?:at|from)\\s+(?:the\\s+)?([A-Za-z][A-Za-z0-9&' ]*?)(?=\\s+(?:for|on|yesterday|today|\\d)|[.,]|$)", Pattern.CASE_INSENSITIVE);
    private static final String[][] CATEGORIES = {
            {"Food", "coffee", "lunch", "dinner", "breakfast", "restaurant", "cafe", "pizza", "burger"},
            {"Groceries", "grocery", "groceries", "supermarket"},
            {"Transport", "uber", "taxi", "bus", "gas", "fuel", "train", "parking"},
            {"Entertainment", "netflix", "spotify", "movie", "cinema", "game"},
            {"Housing", "rent", "mortgage", "utilities", "electricity"},
            {"Health", "pharmacy", "doctor", "dentist"},
            {"Shopping", "amazon", "clothes", "shoes", "shopping"}};

    private final String apiKey;
    private Supplier<String> fallbackInput;
    private final int recordSeconds = 8;

    public VoiceInputService(String apiKey) { this.apiKey = apiKey; }

    public void setFallbackInput(Supplier<String> fallbackInput) { this.fallbackInput = fallbackInput; }

    public String captureSpeech() {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return transcribe(record());
            } catch (LLMUnavailableException | LineUnavailableException | IllegalArgumentException e) {
                if (fallbackInput == null) throw new LLMUnavailableException("Speech-to-Text Service is not available: " + e.getMessage(), e);
            }
        }
        if (fallbackInput == null) throw new LLMUnavailableException("Speech-to-Text Service is not available.");
        return fallbackInput.get();
    }

    /** True when the transcript is clear enough: at least two words and a positive amount. */
    public boolean validateTranscript(String transcript) {
        if (transcript == null || transcript.isBlank() || transcript.trim().split("\\s+").length < 2) return false;
        Matcher m = AMOUNT.matcher(transcript);
        return m.find() && parseAmount(m.group(1)) > 0;
    }

    /** Extracts type, amount, date, merchant and category from a sentence such as "I spent 12.50 on coffee at Tim Hortons". */
    public FinancialEntry convertToEntry(String transcript) {
        String t = transcript.trim();
        String lower = t.toLowerCase();
        Matcher am = AMOUNT.matcher(t);
        double amount = am.find() ? parseAmount(am.group(1)) : 0;
        LocalDate date = LocalDate.now();
        Matcher dm = DATE.matcher(t);
        if (dm.find()) date = LocalDate.parse(dm.group(1));
        else if (lower.contains("yesterday")) date = date.minusDays(1);
        Matcher mm = MERCHANT.matcher(t);
        String merchant = mm.find() ? mm.group(1).trim() : "Unknown";
        boolean income = lower.matches(".*\\b(earned|received|salary|paycheck|income|got paid|paid me|deposit)\\b.*");
        if (income) return new Income(amount, date, t, merchant, "Voice");
        String category = "Uncategorized";
        outer:
        for (String[] row : CATEGORIES)
            for (int i = 1; i < row.length; i++) if (lower.contains(row[i])) { category = row[0]; break outer; }
        return new Expense(amount, date, t, category, merchant);
    }

    private static double parseAmount(String s) { return Double.parseDouble(s.replace(',', '.')); }

    private byte[] record() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
        TargetDataLine line = AudioSystem.getTargetDataLine(format);
        line.open(format);
        line.start();
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        long end = System.currentTimeMillis() + recordSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            int n = line.read(buf, 0, buf.length);
            if (n > 0) pcm.write(buf, 0, n);
        }
        line.stop();
        line.close();
        byte[] data = pcm.toByteArray();
        ByteArrayOutputStream wav = new ByteArrayOutputStream();
        try {
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(data), format, data.length / format.getFrameSize()),
                    AudioFileFormat.Type.WAVE, wav);
        } catch (IOException e) {
            throw new LLMUnavailableException("Could not prepare the audio", e);
        }
        return wav.toByteArray();
    }

    private String transcribe(byte[] wav) {
        String boundary = "----finassist" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(wav);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/transcriptions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new LLMUnavailableException("Speech-to-Text Service returned HTTP " + response.statusCode());
            String text = OpenAILLMService.extractJsonString(response.body(), "text");
            return text == null ? "" : text;
        } catch (IOException e) {
            throw new LLMUnavailableException("Speech-to-Text Service could not be reached", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMUnavailableException("Speech-to-Text request was interrupted", e);
        }
    }
}

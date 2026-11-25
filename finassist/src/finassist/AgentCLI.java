package finassist;

import finassist.exception.LLMUnavailableException;
import finassist.exception.ValidationException;
import finassist.facade.FinAssistFacade;
import finassist.llm.LLMService;
import finassist.llm.MockLLMService;
import finassist.llm.OpenAILLMService;
import finassist.repository.FinancialRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Standalone command-line entry point: runs the FinAssist agent on one task read from a file.
 * Usage: java -jar Agent.jar [-LLMAPI] -input task.txt [-output result.txt] [-data file] [-demo] [-model name] [-help]
 * Exit codes: 0 success, 1 run-time failure, 2 wrong usage.
 */
public class AgentCLI {
    static final String USAGE = String.join("\n",
        "Usage: java -jar Agent.jar [options] -input <task file>",
        "",
        "Options:",
        "  -input <file>    Task to run (plain text). Use - to read the task from standard input. Required.",
        "  -LLMAPI          Use the OpenAI LLM (needs the OPENAI_API_KEY environment variable).",
        "                   Without it the offline stand-in AI is used (no network, answers are placeholders).",
        "  -model <name>    OpenAI model name (default: OPENAI_MODEL or gpt-4o-mini). Only with -LLMAPI.",
        "  -data <file>     Load and save the user's financial data in this file (default: no file, empty data).",
        "  -demo            Add the fictional sample data (salary, lunches, rent, goal, subscription) before running.",
        "  -output <file>   Write the agent's answer to this file instead of the screen.",
        "  -help            Show this help.",
        "",
        "Task examples: \"How can I reduce my food spending?\"  |  \"analyze unusual expense\"",
        "Exit codes: 0 success, 1 failure (for example LLM unavailable), 2 wrong usage.");

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        boolean llmApi = false, demo = false;
        String input = null, output = null, data = null, model = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-help": case "--help": case "-h": System.out.println(USAGE); return 0;
                case "-LLMAPI": llmApi = true; break;
                case "-demo": demo = true; break;
                case "-input": case "-output": case "-data": case "-model":
                    if (i + 1 >= args.length) return usage("Missing value after " + args[i] + ".");
                    String v = args[++i];
                    if (args[i - 1].equals("-input")) input = v;
                    else if (args[i - 1].equals("-output")) output = v;
                    else if (args[i - 1].equals("-data")) data = v;
                    else model = v;
                    break;
                default: return usage("Unknown option: " + args[i]);
            }
        }
        if (input == null) return usage("-input <task file> is required.");

        String task;
        try {
            task = input.equals("-") ? new String(System.in.readAllBytes(), StandardCharsets.UTF_8)
                                     : Files.readString(Path.of(input), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("ERROR: cannot read task file \"" + input + "\": " + e.getMessage());
            return 1;
        }
        task = task.strip();
        if (task.isEmpty()) { System.err.println("ERROR: the task file is empty."); return 1; }

        LLMService llm;
        String key = System.getenv("OPENAI_API_KEY");
        if (llmApi) {
            if (key == null || key.isBlank()) {
                System.err.println("ERROR: -LLMAPI needs the OPENAI_API_KEY environment variable.");
                return 2;
            }
            if (model == null) model = System.getenv("OPENAI_MODEL");
            llm = (model == null || model.isBlank()) ? new OpenAILLMService(key) : new OpenAILLMService(key, model);
        } else {
            llm = new MockLLMService();
            System.err.println("Note: offline stand-in AI in use (add -LLMAPI for the real LLM).");
        }

        try {
            FinancialRepository repository = data == null ? new FinancialRepository() : new FinancialRepository(Path.of(data));
            FinAssistFacade facade = new FinAssistFacade(repository, llm, key);
            if (demo) Main.seed(facade);
            String answer = facade.askCoach(task);
            if (output == null) System.out.println(answer);
            else { Files.writeString(Path.of(output), answer + System.lineSeparator(), StandardCharsets.UTF_8);
                   System.err.println("Answer written to " + output); }
            return 0;
        } catch (LLMUnavailableException e) {
            System.err.println("ERROR: the AI service is unavailable: " + e.getMessage());
        } catch (ValidationException e) {
            System.err.println("ERROR: " + e.getMessage());
        } catch (IOException | RuntimeException e) {
            System.err.println("ERROR: " + e);
        }
        return 1;
    }

    private static int usage(String message) {
        System.err.println("ERROR: " + message + "\n\n" + USAGE);
        return 2;
    }
}

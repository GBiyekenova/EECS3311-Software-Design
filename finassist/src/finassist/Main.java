package finassist;

import finassist.controller.FinancialController;
import finassist.facade.FinAssistFacade;
import finassist.llm.LLMService;
import finassist.llm.MockLLMService;
import finassist.llm.OpenAILLMService;
import finassist.model.*;
import finassist.repository.FinancialRepository;
import finassist.view.CLIView;
import finassist.view.GUIView;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Usage: java finassist.Main [gui|cli] [--demo] [--data file]
 * Set OPENAI_API_KEY to use the real OpenAI GPT and speech-to-text models; without it an offline stand-in is used.
 */
public class Main {
    public static void main(String[] args) {
        boolean cli = false, demo = false;
        String dataFile = "finassist-data.ser";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("cli")) cli = true;
            else if (args[i].equals("--demo")) demo = true;
            else if (args[i].equals("--data") && i + 1 < args.length) dataFile = args[++i];
        }
        String key = System.getenv("OPENAI_API_KEY");
        boolean haveKey = key != null && !key.isBlank();
        LLMService llm = haveKey ? new OpenAILLMService(key) : new MockLLMService();
        FinancialRepository repository = new FinancialRepository(Path.of(dataFile));
        FinAssistFacade facade = new FinAssistFacade(repository, llm, key);
        if (demo) seed(facade);
        FinancialController controller = new FinancialController(facade);

        if (!haveKey) System.out.println("OPENAI_API_KEY is not set: using the offline demo AI and typed voice input.");
        if (cli) new CLIView(controller).run();
        else SwingUtilities.invokeLater(() -> new GUIView(controller).setVisible(true));
    }

    /** Sample data so every feature can be tried straight away. */
    static void seed(FinAssistFacade f) {
        LocalDate today = LocalDate.now();
        f.logIncome(new Income(3000, today.withDayOfMonth(1), "Salary", "Employer", "Salary"));
        double[] food = {12, 15, 11, 14, 13};
        for (int i = 0; i < food.length; i++)
            f.logExpense(new Expense(food[i], today.withDayOfMonth(2 + i), "Lunch", "Food", "Cafe"));
        f.logExpense(new Expense(900, today.withDayOfMonth(1), "Rent", "Housing", "Landlord"));
        f.logExpense(new Expense(120, today.withDayOfMonth(3), "Groceries", "Groceries", "Market"));
        f.addGoal(new FinancialGoal("Vacation", 2000, 500, today.plusMonths(6)));
        f.addSubscription(new Subscription("Netflix", 15.99, "MONTHLY", today.plusDays(10)));
    }
}

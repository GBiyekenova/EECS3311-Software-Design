package finassist.agent;

import finassist.exception.LLMUnavailableException;
import finassist.llm.LLMService;
import finassist.model.Expense;
import finassist.model.FinancialEntry;
import finassist.observer.FinancialObserver;
import finassist.repository.FinancialRepository;
import finassist.view.FinancialView;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects unusual expenses (F03). Factory Method: ConcreteProduct. Observer: ConcreteObserver.
 * Deterministic part: compares the new expense with past spending (same category when there are
 * at least 3 earlier expenses in it, otherwise all expenses). The LLM only writes the explanation.
 */
public class ExpenseDetectionAgent implements FinancialAgent, FinancialObserver {
    private static final int MIN_HISTORY = 3;
    private final FinancialRepository repository;
    private final LLMService llm;
    private FinancialView view;
    private int lastAnalyzedId = -1;

    // statistics of the last comparison, used by analyzeExpense()
    private boolean enoughHistory;
    private double mean, threshold;
    private String basis = "";

    public ExpenseDetectionAgent(FinancialRepository repository, LLMService llm) {
        this.repository = repository;
        this.llm = llm;
    }

    /** The agent shows alerts and errors directly on the view (class diagram: "display results"). */
    public void setView(FinancialView view) { this.view = view; }

    public boolean hasEnoughHistory() { return enoughHistory; }

    public boolean detectUnusualExpense(Expense expense) {
        List<Expense> all = new ArrayList<>();
        List<Expense> sameCategory = new ArrayList<>();
        for (FinancialEntry e : repository.getEntries()) {
            if (e instanceof Expense && e.getEntryId() != expense.getEntryId()) {
                all.add((Expense) e);
                if (((Expense) e).getCategory().equalsIgnoreCase(expense.getCategory())) sameCategory.add((Expense) e);
            }
        }
        List<Expense> base = sameCategory.size() >= MIN_HISTORY ? sameCategory : all;
        basis = sameCategory.size() >= MIN_HISTORY ? "your earlier " + expense.getCategory() + " expenses" : "all your earlier expenses";
        enoughHistory = base.size() >= MIN_HISTORY;
        if (!enoughHistory) return false;

        mean = base.stream().mapToDouble(Expense::getAmount).average().orElse(0);
        double variance = base.stream().mapToDouble(x -> (x.getAmount() - mean) * (x.getAmount() - mean)).average().orElse(0);
        double std = Math.sqrt(variance);
        threshold = std == 0 ? mean * 1.5 : mean + 2 * std;
        boolean unusual = expense.getAmount() > threshold;
        expense.setUnusual(unusual);
        repository.persist();
        return unusual;
    }

    /** Asks the LLM to explain why the expense differs from the usual pattern. */
    public String analyzeExpense(Expense expense) {
        String prompt = String.format("Explain briefly why this expense is unusual.%n"
                        + "Expense: $%.2f at %s (category %s) on %s.%n"
                        + "Average of %s: $%.2f. Unusual above: $%.2f.",
                expense.getAmount(), expense.getMerchant(), expense.getCategory(), expense.getDate(), basis, mean, threshold);
        return llm.generate(prompt);
    }

    /** Observer: called after financial data changed. Checks the newest expense and tells the view. */
    @Override
    public void update() {
        List<FinancialEntry> entries = repository.getEntries();
        if (entries.isEmpty()) return;
        FinancialEntry last = entries.get(entries.size() - 1);
        if (!(last instanceof Expense) || last.getEntryId() == lastAnalyzedId) return;
        lastAnalyzedId = last.getEntryId();
        Expense expense = (Expense) last;
        boolean unusual = detectUnusualExpense(expense);
        if (view == null) return;
        if (!enoughHistory) {
            view.display("Not enough history to tell whether this expense is unusual yet.");
        } else if (unusual) {
            String header = String.format("ALERT: unusual expense of $%.2f at %s.", expense.getAmount(), expense.getMerchant());
            try {
                view.display(header + "\n" + analyzeExpense(expense));
            } catch (LLMUnavailableException e) {
                view.displayError(header + " AI service unavailable, so no explanation could be written.");
            }
        }
    }

    /** Used by the orchestrator: analyse the newest expense. */
    @Override
    public String process(String input) {
        List<FinancialEntry> entries = repository.getEntries();
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i) instanceof Expense) {
                Expense e = (Expense) entries.get(i);
                if (!detectUnusualExpense(e)) return enoughHistory ? "That expense is normal for your spending." : "Not enough history to compare yet.";
                return analyzeExpense(e);
            }
        }
        return "There are no expenses to analyse.";
    }
}

package finassist.service;

import finassist.model.FinancialEntry;
import finassist.model.FinancialGoal;
import finassist.llm.LLMService;
import finassist.observer.FinancialObserver;
import finassist.repository.FinancialRepository;
import finassist.strategy.ExpenseBasedSavingsStrategy;
import finassist.strategy.GoalBasedSavingsStrategy;
import finassist.strategy.SavingsStrategy;

import java.util.List;

/** Strategy pattern: Context (F08). Also an Observer: re-selects its strategy when data changes. */
public class SavingsAnalyzer implements FinancialObserver {
    private final FinancialRepository repository;
    private final LLMService llm;
    private SavingsStrategy strategy;

    public SavingsAnalyzer(FinancialRepository repository, LLMService llm) {
        this.repository = repository;
        this.llm = llm;
        this.strategy = new ExpenseBasedSavingsStrategy();
    }

    public void setStrategy(SavingsStrategy strategy) { this.strategy = strategy; }

    /** Goal-based when the user has goals, otherwise expense-based. */
    public SavingsStrategy selectStrategy() {
        return repository.getGoals().isEmpty() ? new ExpenseBasedSavingsStrategy() : new GoalBasedSavingsStrategy();
    }

    public String analyze() {
        setStrategy(selectStrategy());
        List<FinancialEntry> entries = repository.getEntries();
        List<FinancialGoal> goals = repository.getGoals();
        String recommendation = strategy.calculateRecommendation(entries, goals);

        StringBuilder missing = new StringBuilder();
        if (entries.isEmpty()) missing.append("No income or expenses are recorded. ");
        if (goals.isEmpty()) missing.append("No financial goal exists. ");
        String prompt = "Turn this computed savings analysis into short, personalized suggestions.\n"
                + (missing.length() > 0 ? "Missing data (say so and state your assumptions): " + missing + "\n" : "")
                + recommendation;
        return recommendation + "\n" + llm.generate(prompt);
    }

    @Override public void update() { setStrategy(selectStrategy()); }
}

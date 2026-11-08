package finassist.agent;

import finassist.llm.LLMService;
import finassist.model.*;
import finassist.repository.FinancialRepository;

import java.util.ArrayList;
import java.util.List;

/** AI financial coach (F10, and the written interpretation in F05). Factory Method: ConcreteProduct. */
public class FinancialCoachAgent implements FinancialAgent {
    private final FinancialRepository repository;
    private final LLMService llm;
    private final List<String> history = new ArrayList<>(); // earlier questions and answers, for follow-ups

    public FinancialCoachAgent(FinancialRepository repository, LLMService llm) {
        this.repository = repository;
        this.llm = llm;
    }

    @Override
    public String process(String input) {
        if (input == null || input.trim().split("\\s+").length < 2)
            return "Could you tell me a little more about what you would like to know?";
        if (repository.getEntries().isEmpty() && repository.getGoals().isEmpty() && repository.getSubscriptions().isEmpty())
            return "I do not have any of your financial data yet, so I cannot answer reliably. "
                    + "Please add some income, expenses, goals or subscriptions first, or tell me what you would like to plan.";
        String answer = generateAdvice(input);
        history.add("User: " + input);
        history.add("Coach: " + answer);
        return answer;
    }

    /** Builds the prompt from the question, the user's data and the conversation so far, and asks the LLM. */
    public String generateAdvice(String question) {
        StringBuilder sb = new StringBuilder("Answer the user's question using only the data below. ")
                .append("If the data is not enough, ask a clarifying question instead of guessing.\n\n")
                .append(prepareContext()).append('\n');
        if (!history.isEmpty()) {
            sb.append("Conversation so far:\n");
            for (int i = Math.max(0, history.size() - 6); i < history.size(); i++) sb.append(history.get(i)).append('\n');
        }
        sb.append("\nQuestion: ").append(question);
        return llm.generate(sb.toString());
    }

    /** Collects the user's entries, goals and subscriptions as text for the LLM. */
    public String prepareContext() {
        List<FinancialEntry> entries = repository.getEntries();
        double income = 0, expenses = 0;
        StringBuilder recent = new StringBuilder();
        for (FinancialEntry e : entries) {
            if (e instanceof Income) income += e.getAmount(); else expenses += e.getAmount();
        }
        for (int i = Math.max(0, entries.size() - 10); i < entries.size(); i++) {
            FinancialEntry e = entries.get(i);
            String kind = e instanceof Income ? "income" : "expense " + ((Expense) e).getCategory();
            recent.append(String.format("- %s %s $%.2f%n", e.getDate(), kind, e.getAmount()));
        }
        StringBuilder sb = new StringBuilder(String.format("User data:%nTotal income $%.2f, total expenses $%.2f.%n", income, expenses));
        sb.append("Recent entries:\n").append(recent.length() == 0 ? "- none\n" : recent);
        sb.append("Goals:\n");
        List<FinancialGoal> goals = repository.getGoals();
        if (goals.isEmpty()) sb.append("- none\n");
        for (FinancialGoal g : goals)
            sb.append(String.format("- %s: target $%.2f, saved $%.2f, deadline %s%n", g.getName(), g.getTargetAmount(), g.getCurrentAmount(), g.getDeadline()));
        sb.append("Subscriptions:\n");
        List<Subscription> subs = repository.getSubscriptions();
        if (subs.isEmpty()) sb.append("- none\n");
        for (Subscription s : subs) sb.append(String.format("- %s: $%.2f per month%n", s.getName(), s.getMonthlyCost()));
        return sb.toString();
    }
}

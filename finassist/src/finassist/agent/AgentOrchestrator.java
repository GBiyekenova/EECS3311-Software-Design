package finassist.agent;

import java.util.ArrayList;
import java.util.List;

/** Picks the right FinancialAgent for a request and creates it through the AgentCreator when needed (Factory Method client). */
public class AgentOrchestrator {
    private final List<FinancialAgent> agents = new ArrayList<>();
    private final AgentCreator expenseDetectionCreator;
    private final AgentCreator financialCoachCreator;

    public AgentOrchestrator(AgentCreator expenseDetectionCreator, AgentCreator financialCoachCreator) {
        this.expenseDetectionCreator = expenseDetectionCreator;
        this.financialCoachCreator = financialCoachCreator;
    }

    public String processRequest(String request) { return selectAgent(request).process(request); }

    public FinancialAgent selectAgent(String request) {
        boolean expense = request != null && request.toLowerCase().startsWith("analyze unusual expense");
        Class<? extends FinancialAgent> wanted = expense ? ExpenseDetectionAgent.class : FinancialCoachAgent.class;
        for (FinancialAgent a : agents) if (wanted.isInstance(a)) return a;
        FinancialAgent created = (expense ? expenseDetectionCreator : financialCoachCreator).createAgent();
        addAgent(created);
        return created;
    }

    public void addAgent(FinancialAgent agent) { agents.add(agent); }
}

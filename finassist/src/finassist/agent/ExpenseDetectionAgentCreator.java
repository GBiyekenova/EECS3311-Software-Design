package finassist.agent;

import finassist.llm.LLMService;
import finassist.repository.FinancialRepository;

/** Factory Method pattern: ConcreteCreator for ExpenseDetectionAgent. */
public class ExpenseDetectionAgentCreator extends AgentCreator {
    private final FinancialRepository repository;
    private final LLMService llm;

    public ExpenseDetectionAgentCreator(FinancialRepository repository, LLMService llm) {
        this.repository = repository;
        this.llm = llm;
    }

    @Override public FinancialAgent createAgent() { return new ExpenseDetectionAgent(repository, llm); }
}

package finassist.agent;

import finassist.llm.LLMService;
import finassist.repository.FinancialRepository;

/** Factory Method pattern: ConcreteCreator for FinancialCoachAgent. */
public class FinancialCoachAgentCreator extends AgentCreator {
    private final FinancialRepository repository;
    private final LLMService llm;

    public FinancialCoachAgentCreator(FinancialRepository repository, LLMService llm) {
        this.repository = repository;
        this.llm = llm;
    }

    @Override public FinancialAgent createAgent() { return new FinancialCoachAgent(repository, llm); }
}

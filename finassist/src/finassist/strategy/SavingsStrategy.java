package finassist.strategy;

import finassist.model.FinancialEntry;
import finassist.model.FinancialGoal;

import java.util.List;

/** Strategy pattern: Strategy interface. */
public interface SavingsStrategy {
    String calculateRecommendation(List<FinancialEntry> entries, List<FinancialGoal> goals);
}

package finassist.strategy;

import finassist.model.FinancialEntry;
import finassist.model.FinancialGoal;

import java.util.List;

/** Strategy pattern: ConcreteStrategy that plans around the user's financial goals. */
public class GoalBasedSavingsStrategy implements SavingsStrategy {
    @Override
    public String calculateRecommendation(List<FinancialEntry> entries, List<FinancialGoal> goals) {
        StringBuilder sb = new StringBuilder("Goal-based analysis:\n");
        boolean haveData = !entries.isEmpty();
        double capacity = 0;
        if (haveData) {
            int months = Stats.months(entries);
            capacity = (Stats.total(entries, true) - Stats.total(entries, false)) / months;
            sb.append(String.format("Average monthly savings capacity: $%.2f (over %d month(s) of data).%n", capacity, months));
        } else {
            sb.append("Assumption: no income or expenses recorded yet, so savings capacity is unknown.\n");
        }
        for (FinancialGoal g : goals) {
            double need = g.getRequiredMonthlySavings();
            sb.append(String.format("- %s: $%.2f remaining of $%.2f, deadline %s, needs $%.2f per month.",
                    g.getName(), g.getRemainingAmount(), g.getTargetAmount(), g.getDeadline(), need));
            if (g.getRemainingAmount() <= 0) sb.append(" Goal already reached.");
            else if (haveData) {
                if (capacity >= need) sb.append(String.format(" On track (surplus $%.2f per month).", capacity - need));
                else sb.append(String.format(" Short by $%.2f per month.", need - capacity));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}

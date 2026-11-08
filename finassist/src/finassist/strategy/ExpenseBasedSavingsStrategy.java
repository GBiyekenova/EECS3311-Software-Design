package finassist.strategy;

import finassist.model.Expense;
import finassist.model.FinancialEntry;
import finassist.model.FinancialGoal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Strategy pattern: ConcreteStrategy that looks for savings by reducing spending. */
public class ExpenseBasedSavingsStrategy implements SavingsStrategy {
    @Override
    public String calculateRecommendation(List<FinancialEntry> entries, List<FinancialGoal> goals) {
        StringBuilder sb = new StringBuilder("Expense-based analysis:\n");
        if (!Stats.hasExpenses(entries)) {
            return sb.append("Assumption: no expenses recorded yet, so no spending pattern can be analysed.\n").toString();
        }
        int months = Stats.months(entries);
        Map<String, Double> byCategory = new LinkedHashMap<>();
        for (FinancialEntry e : entries)
            if (e instanceof Expense) byCategory.merge(((Expense) e).getCategory(), e.getAmount(), Double::sum);
        List<Map.Entry<String, Double>> top = byCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())).limit(3).collect(Collectors.toList());
        for (Map.Entry<String, Double> c : top) {
            double monthly = c.getValue() / months;
            sb.append(String.format("- %s: about $%.2f per month. Cutting it by 10%% would save $%.2f per month.%n",
                    c.getKey(), monthly, monthly * 0.10));
        }
        if (goals.isEmpty()) sb.append("No financial goal exists, so savings are not compared with a target.\n");
        return sb.toString();
    }
}

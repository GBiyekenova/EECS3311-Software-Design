package finassist.view;

import finassist.model.Expense;
import finassist.model.FinancialGoal;
import finassist.model.MonthlySummary;

import java.util.Map;

/** Turns a MonthlySummary into text (used by both views). */
final class DashboardFormatter {
    private DashboardFormatter() {}

    static String bar(double fraction, int width) {
        int n = (int) Math.round(Math.max(0, Math.min(1, fraction)) * width);
        return "#".repeat(n) + "-".repeat(width - n);
    }

    static String format(MonthlySummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Income:        $%.2f%nExpenses:      $%.2f%nProfit:        $%.2f%nSavings rate:  %.1f%%  [%s]%n",
                s.getTotalIncome(), s.getTotalExpenses(), s.getProfit(), s.getSavingsRate() * 100, bar(s.getSavingsRate(), 20)));
        if (!s.getCategoryTotals().isEmpty()) {
            sb.append("Spending by category:\n");
            double max = s.getCategoryTotals().values().iterator().next();
            for (Map.Entry<String, Double> e : s.getCategoryTotals().entrySet())
                sb.append(String.format("  %-14s $%8.2f  %s%n", e.getKey(), e.getValue(), bar(e.getValue() / max, 20)));
        }
        if (!s.getUnusualExpenses().isEmpty()) {
            sb.append("Unusual expenses:\n");
            for (Expense x : s.getUnusualExpenses())
                sb.append(String.format("  %s  $%.2f at %s (%s)%n", x.getDate(), x.getAmount(), x.getMerchant(), x.getCategory()));
        }
        sb.append(String.format("Recurring subscriptions: $%.2f per month%n", s.getSubscriptionMonthlyCost()));
        if (!s.getGoals().isEmpty()) {
            sb.append("Goals:\n");
            for (FinancialGoal g : s.getGoals())
                sb.append(String.format("  %-16s %3.0f%% [%s]%n", g.getName(), g.getProgress() * 100, bar(g.getProgress(), 20)));
        }
        if (s.getInterpretation() != null) sb.append("\nAI interpretation:\n").append(s.getInterpretation()).append('\n');
        if (s.getAiNotice() != null) sb.append('\n').append(s.getAiNotice()).append('\n');
        return sb.toString();
    }
}

package finassist.strategy;

import finassist.model.Expense;
import finassist.model.FinancialEntry;
import finassist.model.Income;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small helpers shared by the savings strategies. */
final class Stats {
    private Stats() {}

    static int months(List<FinancialEntry> entries) {
        Set<YearMonth> m = new HashSet<>();
        for (FinancialEntry e : entries) m.add(YearMonth.from(e.getDate()));
        return Math.max(1, m.size());
    }

    static double total(List<FinancialEntry> entries, boolean income) {
        double t = 0;
        for (FinancialEntry e : entries) if ((e instanceof Income) == income) t += e.getAmount();
        return t;
    }

    static boolean hasExpenses(List<FinancialEntry> entries) {
        for (FinancialEntry e : entries) if (e instanceof Expense) return true;
        return false;
    }
}

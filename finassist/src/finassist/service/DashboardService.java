package finassist.service;

import finassist.model.*;
import finassist.observer.FinancialObserver;
import finassist.repository.FinancialRepository;

import java.util.*;
import java.util.stream.Collectors;

/** Builds the monthly dashboard (F05, F07). Observer: refreshes when financial data changes. */
public class DashboardService implements FinancialObserver {
    private final FinancialRepository repository;
    private final Map<String, MonthlySummary> cache = new HashMap<>();

    public DashboardService(FinancialRepository repository) { this.repository = repository; }

    public MonthlySummary generateMonthlyDashboard(int month, int year) {
        String key = year + "-" + month;
        MonthlySummary cached = cache.get(key);
        if (cached != null) return cached;

        List<FinancialEntry> entries = repository.getEntriesByMonth(month, year);
        double income = calculateIncome(entries);
        double expenses = calculateExpenses(entries);
        MonthlySummary summary = new MonthlySummary(income, expenses);
        summary.setEntryCount(entries.size());
        calculateSavingsRate(summary);

        Map<String, Double> byCategory = new HashMap<>();
        List<Expense> unusual = new ArrayList<>();
        for (FinancialEntry e : entries) {
            if (e instanceof Expense) {
                Expense x = (Expense) e;
                byCategory.merge(x.getCategory(), x.getAmount(), Double::sum);
                if (x.isUnusual()) unusual.add(x);
            }
        }
        Map<String, Double> sorted = new LinkedHashMap<>();
        byCategory.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(en -> sorted.put(en.getKey(), en.getValue()));
        summary.setCategoryTotals(sorted);
        summary.setUnusualExpenses(unusual);
        summary.setSubscriptionMonthlyCost(repository.getSubscriptions().stream().mapToDouble(Subscription::getMonthlyCost).sum());
        summary.setGoals(repository.getGoals());
        cache.put(key, summary);
        return summary;
    }

    public double calculateIncome(List<FinancialEntry> entries) {
        return entries.stream().filter(e -> e instanceof Income).mapToDouble(FinancialEntry::getAmount).sum();
    }

    public double calculateExpenses(List<FinancialEntry> entries) {
        return entries.stream().filter(e -> e instanceof Expense).mapToDouble(FinancialEntry::getAmount).sum();
    }

    public double calculateProfit(List<FinancialEntry> entries) { return calculateIncome(entries) - calculateExpenses(entries); }

    public double calculateSavingsRate(MonthlySummary summary) { return summary.calculateSavingsRate(); }

    /** Called by the subject when data changed: the cached dashboards are out of date. */
    @Override public void update() { cache.clear(); }
}

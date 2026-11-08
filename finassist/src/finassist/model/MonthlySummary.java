package finassist.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The numbers behind the dashboard (F05, F07). */
public class MonthlySummary {
    private final double totalIncome;
    private final double totalExpenses;
    private double profit;
    private double savingsRate;

    // extra information shown on the dashboard / end-of-month summary
    private int entryCount;
    private Map<String, Double> categoryTotals = new LinkedHashMap<>();
    private List<Expense> unusualExpenses = new ArrayList<>();
    private double subscriptionMonthlyCost;
    private List<FinancialGoal> goals = new ArrayList<>();
    private String interpretation;   // written by the LLM (F05)
    private String aiNotice;         // e.g. "AI interpretation unavailable"

    public MonthlySummary(double totalIncome, double totalExpenses) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        calculateProfit();
        calculateSavingsRate();
    }

    public double calculateProfit() { profit = totalIncome - totalExpenses; return profit; }

    public double calculateSavingsRate() {
        savingsRate = totalIncome <= 0 ? 0 : (totalIncome - totalExpenses) / totalIncome;
        return savingsRate;
    }

    public double getTotalIncome() { return totalIncome; }
    public double getTotalExpenses() { return totalExpenses; }
    public double getProfit() { return profit; }
    public double getSavingsRate() { return savingsRate; }

    public boolean isEmpty() { return entryCount == 0; }
    public int getEntryCount() { return entryCount; }
    public void setEntryCount(int entryCount) { this.entryCount = entryCount; }
    public Map<String, Double> getCategoryTotals() { return categoryTotals; }
    public void setCategoryTotals(Map<String, Double> m) { this.categoryTotals = m; }
    public List<Expense> getUnusualExpenses() { return unusualExpenses; }
    public void setUnusualExpenses(List<Expense> l) { this.unusualExpenses = l; }
    public double getSubscriptionMonthlyCost() { return subscriptionMonthlyCost; }
    public void setSubscriptionMonthlyCost(double c) { this.subscriptionMonthlyCost = c; }
    public List<FinancialGoal> getGoals() { return goals; }
    public void setGoals(List<FinancialGoal> g) { this.goals = g; }
    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String s) { this.interpretation = s; }
    public String getAiNotice() { return aiNotice; }
    public void setAiNotice(String s) { this.aiNotice = s; }

    /** Plain-text description, also used as LLM context. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Income $%.2f, expenses $%.2f, profit $%.2f, savings rate %.1f%%.%n",
                totalIncome, totalExpenses, profit, savingsRate * 100));
        if (!categoryTotals.isEmpty()) sb.append("Spending by category: ").append(categoryTotals).append('\n');
        if (!unusualExpenses.isEmpty()) sb.append("Unusual expenses: ").append(unusualExpenses.size()).append('\n');
        sb.append(String.format("Recurring subscriptions: $%.2f per month.%n", subscriptionMonthlyCost));
        return sb.toString();
    }
}

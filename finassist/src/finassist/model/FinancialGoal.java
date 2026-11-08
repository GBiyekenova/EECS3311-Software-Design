package finassist.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FinancialGoal implements Serializable {
    private int goalId;
    private final String name;
    private final double targetAmount;
    private final double currentAmount;
    private final LocalDate deadline;

    public FinancialGoal(String name, double targetAmount, double currentAmount, LocalDate deadline) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
    }

    public int getGoalId() { return goalId; }
    public void setGoalId(int goalId) { this.goalId = goalId; }
    public String getName() { return name; }
    public double getTargetAmount() { return targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public LocalDate getDeadline() { return deadline; }

    /** Progress as a fraction between 0 and 1. */
    public double getProgress() { return targetAmount <= 0 ? 0 : Math.min(1.0, currentAmount / targetAmount); }

    public double getRemainingAmount() { return Math.max(0, targetAmount - currentAmount); }

    /** Remaining amount divided by the (rounded-up) number of months left, at least 1. */
    public double getRequiredMonthlySavings() {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        long months = Math.max(1, (long) Math.ceil(days / 30.0));
        return getRemainingAmount() / months;
    }
}

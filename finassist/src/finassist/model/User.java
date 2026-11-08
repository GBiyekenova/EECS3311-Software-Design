package finassist.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Owns the financial entries, goals and subscriptions (MVC: Model). */
public class User implements Serializable {
    private final int userId;
    private final String name;
    private final List<FinancialEntry> entries = new ArrayList<>();
    private final List<FinancialGoal> goals = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();

    public User(int userId, String name) { this.userId = userId; this.name = name; }

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public List<FinancialEntry> getFinancialEntries() { return entries; }
    public List<FinancialGoal> getFinancialGoals() { return goals; }
    public List<Subscription> getSubscriptions() { return subscriptions; }
}

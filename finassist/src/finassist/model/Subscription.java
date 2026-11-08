package finassist.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Subscription implements Serializable {
    private int subscriptionId;
    private final String name;
    private final double amount;
    private final String frequency; // WEEKLY, MONTHLY or YEARLY
    private final LocalDate billingDate;

    public Subscription(String name, double amount, String frequency, LocalDate billingDate) {
        this.name = name;
        this.amount = amount;
        this.frequency = frequency == null ? "" : frequency.toUpperCase();
        this.billingDate = billingDate;
    }

    public int getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(int id) { this.subscriptionId = id; }
    public String getName() { return name; }
    public double getAmount() { return amount; }
    public String getFrequency() { return frequency; }
    public LocalDate getBillingDate() { return billingDate; }

    public double getMonthlyCost() {
        switch (frequency) {
            case "YEARLY": return amount / 12;
            case "WEEKLY": return amount * 52 / 12;
            default: return amount;
        }
    }

    public double getAnnualCost() { return getMonthlyCost() * 12; }
}

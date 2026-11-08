package finassist.model;

import java.io.Serializable;
import java.time.LocalDate;

/** Abstract parent of Income and Expense (MVC: Model). */
public abstract class FinancialEntry implements Serializable {
    private int entryId;
    private final double amount;
    private final LocalDate date;
    private final String description;

    protected FinancialEntry(double amount, LocalDate date, String description) {
        this.amount = amount;
        this.date = date;
        this.description = description == null ? "" : description;
    }

    public int getEntryId() { return entryId; }
    public void setEntryId(int entryId) { this.entryId = entryId; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
}

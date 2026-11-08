package finassist.model;

import java.time.LocalDate;

public class Income extends FinancialEntry {
    private final String source;
    private final String type;

    public Income(double amount, LocalDate date, String description, String source, String type) {
        super(amount, date, description);
        this.source = source;
        this.type = type;
    }

    @Override public double getAmount() { return super.getAmount(); }
    public String getSource() { return source; }
    public String getType() { return type; }
}

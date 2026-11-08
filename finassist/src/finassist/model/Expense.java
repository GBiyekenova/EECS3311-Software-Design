package finassist.model;

import java.time.LocalDate;

public class Expense extends FinancialEntry {
    private final String category;
    private final String merchant;
    private boolean unusual;

    public Expense(double amount, LocalDate date, String description, String category, String merchant) {
        super(amount, date, description);
        this.category = category;
        this.merchant = merchant;
    }

    @Override public double getAmount() { return super.getAmount(); }
    public String getCategory() { return category; }
    public String getMerchant() { return merchant; }
    public boolean isUnusual() { return unusual; }
    public void setUnusual(boolean unusual) { this.unusual = unusual; }
}

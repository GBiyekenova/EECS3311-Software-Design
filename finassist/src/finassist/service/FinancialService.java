package finassist.service;

import finassist.exception.ValidationException;
import finassist.model.Expense;
import finassist.model.FinancialEntry;
import finassist.model.Income;
import finassist.observer.FinancialDataSubject;
import finassist.repository.FinancialRepository;

import java.time.LocalDate;
import java.util.List;

/** Records income and expenses (F02, F04). It is the Subject of the Observer pattern (F03, F07). */
public class FinancialService extends FinancialDataSubject {
    private final FinancialRepository repository;

    public FinancialService(FinancialRepository repository) { this.repository = repository; }

    public void addIncome(Income income) {
        requirePositive(income.getAmount());
        requireDate(income.getDate());
        requireText(income.getSource(), "source");
        requireText(income.getType(), "type");
        repository.saveEntry(income);
        notifyObservers();
    }

    public void addExpense(Expense expense) {
        requirePositive(expense.getAmount());
        requireDate(expense.getDate());
        requireText(expense.getCategory(), "category");
        requireText(expense.getMerchant(), "merchant");
        repository.saveEntry(expense);
        notifyObservers();
    }

    /** Total income minus total expenses over all entries. */
    public double calculateProfit() {
        double profit = 0;
        for (FinancialEntry e : repository.getEntries()) profit += (e instanceof Income) ? e.getAmount() : -e.getAmount();
        return profit;
    }

    public List<FinancialEntry> getMonthlyEntries(int month, int year) { return repository.getEntriesByMonth(month, year); }

    private static void requirePositive(double amount) {
        if (Double.isNaN(amount) || amount <= 0) throw new ValidationException("The amount must be a positive number.");
    }

    private static void requireDate(LocalDate date) {
        if (date == null) throw new ValidationException("The date is required.");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new ValidationException("The " + field + " is required.");
    }
}

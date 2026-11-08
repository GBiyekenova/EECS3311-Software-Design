package finassist.controller;

import finassist.exception.LLMUnavailableException;
import finassist.exception.ValidationException;
import finassist.facade.FinAssistFacade;
import finassist.model.*;
import finassist.view.FinancialView;

/** MVC: Controller. Receives actions from the view, calls the facade and shows the result on the view. */
public class FinancialController {
    private final FinAssistFacade facade;
    private FinancialView view;

    public FinancialController(FinAssistFacade facade) { this.facade = facade; }

    public void setView(FinancialView view) {
        this.view = view;
        facade.setView(view);
    }

    public void logIncome(Income income) {
        run(() -> { facade.logIncome(income); view.display(String.format("Income saved: $%.2f", income.getAmount())); });
    }

    public void logExpense(Expense expense) {
        run(() -> { facade.logExpense(expense); view.display(String.format("Expense saved: $%.2f", expense.getAmount())); });
    }

    public void logVoiceEntry() {
        run(() -> {
            try {
                facade.logVoiceEntry();
            } catch (ValidationException | LLMUnavailableException e) {
                view.displayError(e.getMessage() + " You can also enter the transaction manually.");
                return;
            }
            FinancialEntry e = facade.getPendingVoiceEntry();
            String kind = e instanceof Income ? "Income from " + ((Income) e).getSource()
                    : "Expense (" + ((Expense) e).getCategory() + ") at " + ((Expense) e).getMerchant();
            view.display(String.format("I understood: %s, $%.2f on %s. Save it? (yes/no)", kind, e.getAmount(), e.getDate()));
            String answer = view.getUserInput();
            boolean yes = answer != null && answer.trim().toLowerCase().startsWith("y");
            facade.confirmVoiceEntry(yes);
            view.display(yes ? "Entry saved." : "Entry discarded. You can repeat it or correct it manually.");
        });
    }

    public void requestDashboard(int month, int year) {
        run(() -> show(facade.getDashboard(month, year)));
    }

    /** End-of-month summary: the dashboard plus an AI interpretation. */
    public void requestMonthlySummary(int month, int year) {
        run(() -> show(facade.getMonthlySummary(month, year)));
    }

    public void addGoal(FinancialGoal goal) {
        run(() -> { facade.addGoal(goal); view.display("Goal saved.\n" + facade.getGoalSummary()); });
    }

    public void requestGoalSummary() { run(() -> view.display(facade.getGoalSummary())); }
    public void requestSavingsAdvice() { run(() -> view.display(facade.getSavingsAdvice())); }
    public void askFinancialCoach(String question) { run(() -> view.display("Coach: " + facade.askCoach(question))); }

    public void manageSubscription(Subscription subscription) {
        run(() -> { facade.addSubscription(subscription); view.display(facade.getSubscriptionSummary()); });
    }

    public void removeSubscription(int id) {
        run(() -> { facade.removeSubscription(id); view.display("Subscription removed.\n" + facade.getSubscriptionSummary()); });
    }

    public void requestSubscriptions() { run(() -> view.display(facade.getSubscriptionSummary())); }

    private void show(MonthlySummary summary) {
        if (summary.isEmpty()) view.display("No financial data for this month. Add an income or expense to get started.");
        else view.showDashboard(summary);
    }

    /** Runs an action and turns failures into messages on the view. */
    private void run(Runnable action) {
        try {
            action.run();
        } catch (ValidationException e) {
            view.displayError(e.getMessage());
        } catch (LLMUnavailableException e) {
            view.displayError("AI service unavailable: " + e.getMessage());
        } catch (RuntimeException e) {
            view.displayError("Unexpected error: " + e);
        }
    }
}

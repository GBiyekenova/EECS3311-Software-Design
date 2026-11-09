package finassist.view;

import finassist.controller.FinancialController;
import finassist.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** MVC: concrete View for the command line. */
public class CLIView implements FinancialView {
    private final FinancialController controller;
    private final Scanner in = new Scanner(System.in);

    public CLIView(FinancialController controller) {
        this.controller = controller;
        controller.setView(this);
    }

    @Override public void display(String data) { System.out.println(data); }
    @Override public void displayError(String message) { System.out.println("ERROR: " + message); }
    @Override public void showDashboard(MonthlySummary summary) { System.out.print(DashboardFormatter.format(summary)); }

    @Override
    public String getUserInput() {
        System.out.print("> ");
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    public void run() {
        System.out.println("FinAssist - personal finance assistant");
        while (true) {
            System.out.println("\n 1 Log expense        2 Log income          3 Voice entry");
            System.out.println(" 4 Dashboard          5 End-of-month summary (AI)");
            System.out.println(" 6 Add goal           7 Goal summary         8 Savings advice (AI)");
            System.out.println(" 9 Add subscription  10 Remove subscription 11 List subscriptions");
            System.out.println("12 Ask AI coach       0 Exit");
            System.out.print("Choose: ");
            if (!in.hasNextLine()) return; // input ended (for example piped input): leave the menu
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1": logExpense(); break;
                    case "2": logIncome(); break;
                    case "3": controller.logVoiceEntry(); break;
                    case "4": controller.requestDashboard(integer("Month (1-12): "), integer("Year: ")); break;
                    case "5": controller.requestMonthlySummary(integer("Month (1-12): "), integer("Year: ")); break;
                    case "6": addGoal(); break;
                    case "7": controller.requestGoalSummary(); break;
                    case "8": controller.requestSavingsAdvice(); break;
                    case "9": addSubscription(); break;
                    case "10": controller.removeSubscription(integer("Subscription number: ")); break;
                    case "11": controller.requestSubscriptions(); break;
                    case "12": coach(); break;
                    case "0": return;
                    default: displayError("Unknown option.");
                }
            } catch (IllegalArgumentException | DateTimeParseException e) {
                displayError("Invalid input. Amounts and numbers must be numeric and dates look like 2026-11-06.");
            }
        }
    }

    private String ask(String prompt) { System.out.print(prompt); return in.hasNextLine() ? in.nextLine().trim() : ""; }
    private double number(String prompt) { return Double.parseDouble(ask(prompt)); }
    private int integer(String prompt) { return Integer.parseInt(ask(prompt)); }
    private LocalDate date(String prompt) { return LocalDate.parse(ask(prompt)); }

    private void logExpense() {
        double amount = number("Amount: "); LocalDate date = date("Date (YYYY-MM-DD): ");
        String category = ask("Category: "), merchant = ask("Merchant: "), description = ask("Description (optional): ");
        controller.logExpense(new Expense(amount, date, description, category, merchant));
    }

    private void logIncome() {
        double amount = number("Amount: "); LocalDate date = date("Date (YYYY-MM-DD): ");
        String source = ask("Source: "), type = ask("Type (e.g. salary, profit): "), description = ask("Description (optional): ");
        controller.logIncome(new Income(amount, date, description, source, type));
    }

    private void addGoal() {
        String name = ask("Goal name: "); double target = number("Target amount: ");
        double current = number("Current savings: "); LocalDate deadline = date("Deadline (YYYY-MM-DD): ");
        controller.addGoal(new FinancialGoal(name, target, current, deadline));
    }

    private void addSubscription() {
        String name = ask("Name: "); double amount = number("Amount: ");
        String freq = ask("Frequency (WEEKLY/MONTHLY/YEARLY): "); LocalDate billing = date("Next billing date (YYYY-MM-DD): ");
        controller.manageSubscription(new Subscription(name, amount, freq, billing));
    }

    private void coach() {
        System.out.println("AI coach - ask questions, empty line to leave.");
        while (true) {
            String q = ask("You: ");
            if (q.isEmpty()) return;
            controller.askFinancialCoach(q);
        }
    }
}

package finassist.test;

import finassist.agent.*;
import finassist.controller.FinancialController;
import finassist.exception.ValidationException;
import finassist.facade.FinAssistFacade;
import finassist.llm.MockLLMService;
import finassist.model.*;
import finassist.observer.FinancialDataSubject;
import finassist.observer.FinancialObserver;
import finassist.repository.FinancialRepository;
import finassist.strategy.*;
import finassist.service.*;
import finassist.view.FinancialView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Run with: java -ea -cp out finassist.test.SelfTest */
public class SelfTest {
    static class RecordingView implements FinancialView {
        final List<String> shown = new ArrayList<>(), errors = new ArrayList<>();
        MonthlySummary dashboard;
        final java.util.ArrayDeque<String> inputs = new java.util.ArrayDeque<>();
        public void display(String d) { shown.add(d); }
        public void displayError(String m) { errors.add(m); }
        public void showDashboard(MonthlySummary s) { dashboard = s; }
        public String getUserInput() { return inputs.isEmpty() ? "" : inputs.poll(); }
        boolean shownContains(String t) { return shown.stream().anyMatch(s -> s.contains(t)); }
    }

    static int passed = 0;
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("ok  " + name);
    }

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        MockLLMService llm = new MockLLMService();
        FinancialRepository repo = new FinancialRepository();
        FinAssistFacade facade = new FinAssistFacade(repo, llm, null);
        FinancialController controller = new FinancialController(facade);
        RecordingView view = new RecordingView();
        controller.setView(view);

        // F02 / F04 + validation
        controller.logExpense(new Expense(-5, today, "", "Food", "Cafe"));
        check(!view.errors.isEmpty(), "F02 invalid amount is rejected");
        controller.logExpense(new Expense(5, today, "", "", "Cafe"));
        check(view.errors.size() == 2, "F02 missing category is rejected");
        controller.logIncome(new Income(3000, today, "pay", "Employer", "Salary"));
        check(view.shownContains("Income saved"), "F04 income saved");
        check(repo.getEntries().size() == 1, "repository stores the income");

        // F03 insufficient history, then unusual detection (Observer + Factory + LLM)
        controller.logExpense(new Expense(10, today, "", "Food", "Cafe"));
        check(view.shownContains("Not enough history"), "F03 insufficient history message");
        controller.logExpense(new Expense(11, today, "", "Food", "Cafe"));
        controller.logExpense(new Expense(12, today, "", "Food", "Cafe"));
        controller.logExpense(new Expense(10, today, "", "Food", "Cafe"));
        view.shown.clear();
        controller.logExpense(new Expense(200, today, "", "Food", "Fancy Place"));
        check(view.shownContains("ALERT: unusual expense"), "F03 unusual expense alert with AI explanation");
        view.shown.clear();
        controller.logExpense(new Expense(11, today, "", "Food", "Cafe"));
        check(!view.shownContains("ALERT"), "F03 normal expense gives no alert");
        llm.setAvailable(false);
        controller.logExpense(new Expense(500, today, "", "Food", "Fancy Place"));
        check(view.errors.stream().anyMatch(e -> e.contains("AI service unavailable")), "F03 LLM unavailable shows an error");
        llm.setAvailable(true);

        // F07 / F05 dashboard
        controller.requestDashboard(today.getMonthValue(), today.getYear());
        check(view.dashboard != null && view.dashboard.getTotalIncome() == 3000, "F07 dashboard income");
        check(Math.abs(view.dashboard.getTotalExpenses() - (10 + 11 + 12 + 10 + 200 + 11 + 500)) < 1e-9, "F07 dashboard expenses");
        check(view.dashboard.getUnusualExpenses().size() == 2, "F07 dashboard lists unusual expenses");
        check(view.dashboard.getCategoryTotals().containsKey("Food"), "F07 spending by category");
        view.shown.clear();
        controller.requestDashboard(1, today.getYear() - 5);
        check(view.shownContains("No financial data"), "F07 empty dashboard message");
        controller.requestMonthlySummary(today.getMonthValue(), today.getYear());
        check(view.dashboard.getInterpretation() != null && view.dashboard.getInterpretation().contains("Offline demo AI"), "F05 AI interpretation attached (written by the coach agent)");
        llm.setAvailable(false);
        controller.requestMonthlySummary(today.getMonthValue(), today.getYear());
        check(view.dashboard.getInterpretation() == null && view.dashboard.getAiNotice() != null, "F05 works without the LLM");
        llm.setAvailable(true);

        // Observer: dashboard cache refreshes when data changes
        double before = facade.getDashboard(today.getMonthValue(), today.getYear()).getTotalIncome();
        controller.logIncome(new Income(100, today, "", "Side job", "Profit"));
        check(facade.getDashboard(today.getMonthValue(), today.getYear()).getTotalIncome() == before + 100, "Observer refreshes the dashboard");

        // F06 goals
        controller.addGoal(new FinancialGoal("Old", 100, 0, today.minusDays(1)));
        check(view.errors.stream().anyMatch(e -> e.contains("deadline has passed")), "F06 expired deadline rejected");
        controller.addGoal(new FinancialGoal("Laptop", 1200, 200, today.plusMonths(5)));
        check(view.shownContains("Laptop"), "F06 goal summary shown");
        FinancialGoal g = repo.getGoals().get(0);
        check(Math.abs(g.getProgress() - 200.0 / 1200) < 1e-9 && g.getRemainingAmount() == 1000, "F06 progress and remaining");
        check(g.getRequiredMonthlySavings() > 150 && g.getRequiredMonthlySavings() < 250, "F06 required monthly savings");

        // F08 strategy
        SavingsAnalyzer analyzer = new SavingsAnalyzer(repo, llm);
        check(analyzer.selectStrategy() instanceof GoalBasedSavingsStrategy, "Strategy: goals -> GoalBasedSavingsStrategy");
        check(new SavingsAnalyzer(new FinancialRepository(), llm).selectStrategy() instanceof ExpenseBasedSavingsStrategy, "Strategy: no goals -> ExpenseBasedSavingsStrategy");
        view.shown.clear();
        controller.requestSavingsAdvice();
        check(view.shownContains("Goal-based analysis"), "F08 savings advice uses the goal strategy");

        // F09 subscriptions
        controller.manageSubscription(new Subscription("Netflix", 15.99, "MONTHLY", today));
        controller.manageSubscription(new Subscription("Domain", 120, "YEARLY", today));
        check(view.shownContains("Total: $25.99 per month"), "F09 monthly total");
        controller.manageSubscription(new Subscription("Bad", 0, "MONTHLY", today));
        check(view.errors.stream().anyMatch(e -> e.contains("positive")), "F09 invalid subscription rejected");
        controller.removeSubscription(99);
        check(view.errors.stream().anyMatch(e -> e.contains("does not exist")), "F09 removing a missing subscription is an error");
        controller.removeSubscription(1);
        check(repo.getSubscriptions().size() == 1, "F09 subscription removed");
        check(Math.abs(new Subscription("x", 120, "YEARLY", today).getAnnualCost() - 120) < 1e-9, "F09 annual cost");

        // F10 coach
        view.shown.clear();
        controller.askFinancialCoach("hi");
        check(view.shownContains("tell me a little more"), "F10 unclear question -> clarification");
        controller.askFinancialCoach("How can I save more money each month?");
        check(view.shownContains("Coach: [Offline demo AI"), "F10 coach answer through the LLM");
        check(new FinancialCoachAgent(new FinancialRepository(), llm).process("What should I do about my budget?").contains("do not have any"),
                "F10 no data -> asks instead of guessing");

        // F01 voice (typed fallback)
        view.shown.clear();
        view.inputs.add("I spent 12.50 on coffee at Tim Hortons"); view.inputs.add("yes");
        VoiceInputService voice = new VoiceInputService(null);
        check(voice.validateTranscript("I spent 12.50 on coffee at Tim Hortons"), "F01 transcript validation");
        check(!voice.validateTranscript("um"), "F01 unclear transcript rejected");
        Expense e = (Expense) voice.convertToEntry("I spent 12.50 on coffee at Tim Hortons");
        check(e.getAmount() == 12.50 && e.getCategory().equals("Food") && e.getMerchant().equals("Tim Hortons"), "F01 convertToEntry expense");
        check(voice.convertToEntry("I received 500 from my employer") instanceof Income, "F01 convertToEntry income");
        int before2 = repo.getEntries().size();
        controller.logVoiceEntry();   // first input is used as the sentence, second call as the confirmation 
        check(view.shownContains("I understood"), "F01 voice entry asks for confirmation");
        check(repo.getEntries().size() == before2 + 1, "F01 confirmed voice entry is saved");
        view.inputs.add("um");
        controller.logVoiceEntry();
        check(view.errors.stream().anyMatch(m -> m.contains("could not understand")), "F01 unclear speech -> repeat or enter manually");

        // Facade / Factory Method / Observer structure
        AgentOrchestrator orch = new AgentOrchestrator(new ExpenseDetectionAgentCreator(repo, llm), new FinancialCoachAgentCreator(repo, llm));
        check(orch.selectAgent("analyze unusual expense") instanceof ExpenseDetectionAgent, "Factory Method: expense agent created on demand");
        FinancialAgent coach = orch.selectAgent("help me budget");
        check(coach instanceof FinancialCoachAgent && orch.selectAgent("another question") == coach, "Factory Method: coach agent created once and reused");
        check(new ExpenseDetectionAgentCreator(repo, llm) instanceof AgentCreator, "AgentCreator is the abstract Creator");
        FinancialDataSubject subject = new FinancialDataSubject();
        int[] calls = {0};
        FinancialObserver o = () -> calls[0]++;
        subject.addObserver(o); subject.notifyObservers(); subject.removeObserver(o); subject.notifyObservers();
        check(calls[0] == 1, "Observer: add / notify / remove");
        check(new FinancialService(repo) instanceof FinancialDataSubject, "FinancialService is the Observer Subject");
        check(new DashboardService(repo) instanceof FinancialObserver && analyzer instanceof FinancialObserver, "DashboardService and SavingsAnalyzer are Observers");
        try { facade.getDashboard(13, 2026); check(false, "month 13"); } catch (ValidationException ex) { check(true, "facade validates the month"); }

        System.out.println("\nAll " + passed + " checks passed.");
    }
}

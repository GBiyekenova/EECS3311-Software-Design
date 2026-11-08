package finassist.facade;

import finassist.agent.*;
import finassist.exception.ValidationException;
import finassist.llm.LLMService;
import finassist.exception.LLMUnavailableException;
import finassist.model.*;
import finassist.repository.FinancialRepository;
import finassist.service.*;
import finassist.view.FinancialView;

import java.time.LocalDate;
import java.util.List;

/** Facade pattern: the single entry point the controller uses to reach all services and agents. */
public class FinAssistFacade {
    private final FinancialService financialService;
    private final DashboardService dashboardService;
    private final SubscriptionService subscriptionService;
    private final AgentOrchestrator agentOrchestrator;
    private final VoiceInputService voiceInputService;
    private final SavingsAnalyzer savingsAnalyzer;
    private final FinancialRepository repository;
    private final ExpenseDetectionAgent expenseDetectionAgent;
    private FinancialEntry pendingVoiceEntry;

    public FinAssistFacade(FinancialRepository repository, LLMService llm, String speechApiKey) {
        this.repository = repository;
        this.financialService = new FinancialService(repository);
        this.dashboardService = new DashboardService(repository);
        this.subscriptionService = new SubscriptionService(repository);
        this.savingsAnalyzer = new SavingsAnalyzer(repository, llm);
        this.voiceInputService = new VoiceInputService(speechApiKey);

        // Factory Method: the creators build the agents, the orchestrator is their client
        AgentCreator expenseCreator = new ExpenseDetectionAgentCreator(repository, llm);
        AgentCreator coachCreator = new FinancialCoachAgentCreator(repository, llm);
        this.agentOrchestrator = new AgentOrchestrator(expenseCreator, coachCreator);
        this.expenseDetectionAgent = (ExpenseDetectionAgent) expenseCreator.createAgent();
        agentOrchestrator.addAgent(expenseDetectionAgent);

        // Observer: these react whenever FinancialService saves an entry
        financialService.addObserver(dashboardService);
        financialService.addObserver(savingsAnalyzer);
        financialService.addObserver(expenseDetectionAgent);
    }

    /** Lets the agent show its alerts, and lets voice entry fall back to typed text. */
    public void setView(FinancialView view) {
        expenseDetectionAgent.setView(view);
        voiceInputService.setFallbackInput(() -> {
            view.display("Speech-to-Text is not available here. Type what you would say, e.g. \"I spent 12.50 on coffee at Tim Hortons\":");
            return view.getUserInput();
        });
    }

    // ---- F02, F04
    public void logIncome(Income income) { financialService.addIncome(income); }
    public void logExpense(Expense expense) { financialService.addExpense(expense); }

    // ---- F01: capture, validate and convert; the entry is saved only after confirmVoiceEntry
    public void logVoiceEntry() {
        pendingVoiceEntry = null;
        String transcript = voiceInputService.captureSpeech();
        if (!voiceInputService.validateTranscript(transcript))
            throw new ValidationException("I could not understand a transaction in \"" + transcript + "\". Please repeat it with an amount.");
        pendingVoiceEntry = voiceInputService.convertToEntry(transcript);
    }

    public FinancialEntry getPendingVoiceEntry() { return pendingVoiceEntry; }

    public void confirmVoiceEntry(boolean confirmed) {
        FinancialEntry entry = pendingVoiceEntry;
        pendingVoiceEntry = null;
        if (!confirmed || entry == null) return;
        if (entry instanceof Income) logIncome((Income) entry); else logExpense((Expense) entry);
    }

    // ---- F07
    public MonthlySummary getDashboard(int month, int year) {
        if (month < 1 || month > 12) throw new ValidationException("The month must be between 1 and 12.");
        return dashboardService.generateMonthlyDashboard(month, year);
    }

    // ---- F05: the dashboard figures plus an LLM-written interpretation
    public MonthlySummary getMonthlySummary(int month, int year) {
        MonthlySummary summary = getDashboard(month, year);
        if (summary.isEmpty()) return summary;
        StringBuilder missing = new StringBuilder();
        if (summary.getGoals().isEmpty()) missing.append("No goals are set. ");
        if (summary.getSubscriptionMonthlyCost() == 0) missing.append("No subscriptions are recorded. ");
        if (summary.getTotalIncome() == 0) missing.append("No income was recorded this month. ");
        String request = "Write a short end-of-month interpretation of this summary for the user. "
                + (missing.length() > 0 ? "Name the missing data: " + missing : "") + "\n" + summary.describe();
        try {
            summary.setInterpretation(agentOrchestrator.processRequest(request));
            summary.setAiNotice(null);
        } catch (LLMUnavailableException e) {
            summary.setInterpretation(null);
            summary.setAiNotice("AI interpretation is unavailable right now. The figures above are still correct.");
        }
        return summary;
    }

    // ---- F06
    public void addGoal(FinancialGoal goal) {
        if (goal.getName() == null || goal.getName().isBlank()) throw new ValidationException("The goal name is required.");
        if (goal.getTargetAmount() <= 0) throw new ValidationException("The target amount must be a positive number.");
        if (goal.getCurrentAmount() < 0) throw new ValidationException("The current savings cannot be negative.");
        if (goal.getDeadline() == null || !goal.getDeadline().isAfter(LocalDate.now()))
            throw new ValidationException("The deadline has passed. Please update the goal with a future date.");
        repository.saveGoal(goal);
    }

    public String getGoalSummary() {
        List<FinancialGoal> goals = repository.getGoals();
        if (goals.isEmpty()) return "You have no financial goals yet. Create one to see your progress.";
        StringBuilder sb = new StringBuilder("Financial goals:\n");
        for (FinancialGoal g : goals)
            sb.append(String.format("- %s: %.0f%% done, $%.2f remaining of $%.2f, deadline %s, save $%.2f per month%n",
                    g.getName(), g.getProgress() * 100, g.getRemainingAmount(), g.getTargetAmount(), g.getDeadline(), g.getRequiredMonthlySavings()));
        return sb.toString();
    }

    // ---- F08, F10
    public String getSavingsAdvice() { return savingsAnalyzer.analyze(); }
    public String askCoach(String question) { return agentOrchestrator.processRequest(question); }

    // ---- F09
    public void addSubscription(Subscription subscription) { subscriptionService.addSubscription(subscription); }
    public void removeSubscription(int id) { subscriptionService.removeSubscription(id); }

    public String getSubscriptionSummary() {
        List<Subscription> subs = subscriptionService.getSubscriptions();
        if (subs.isEmpty()) return "You have no subscriptions.";
        StringBuilder sb = new StringBuilder("Subscriptions:\n");
        for (Subscription s : subs)
            sb.append(String.format("- #%d %s: $%.2f/month, $%.2f/year (%s)%n", s.getSubscriptionId(), s.getName(), s.getMonthlyCost(), s.getAnnualCost(), s.getFrequency()));
        sb.append(String.format("Total: $%.2f per month, $%.2f per year", subscriptionService.calculateMonthlyCost(), subscriptionService.calculateMonthlyCost() * 12));
        return sb.toString();
    }
}

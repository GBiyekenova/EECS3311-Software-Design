package finassist.view;

import finassist.controller.FinancialController;
import finassist.model.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/** MVC: concrete View for the graphical interface (Swing). Controller calls run on a worker thread so the window stays responsive. */
public class GUIView extends JFrame implements FinancialView {
    private final FinancialController controller;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    private final JTextArea output = new JTextArea(12, 60);
    private final JLabel status = new JLabel(" ");
    private final ChartPanel chart = new ChartPanel();
    private final JProgressBar savingsBar = new JProgressBar(0, 100);
    private String lastMessage = "";

    // log entry form
    private final JComboBox<String> kind = new JComboBox<>(new String[]{"Expense", "Income"});
    private final JTextField amount = new JTextField(10), date = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField field1 = new JTextField(15), field2 = new JTextField(15), description = new JTextField(20);
    private final JLabel label1 = new JLabel("Category"), label2 = new JLabel("Merchant");
    private final JSpinner month = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
    private final JSpinner year = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
    private final JTextField coachQuestion = new JTextField(40);

    public GUIView(FinancialController controller) {
        super("FinAssist");
        this.controller = controller;
        controller.setView(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        year.setEditor(new JSpinner.NumberEditor(year, "#"));

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Log entry", logPanel());
        tabs.add("Dashboard", dashboardPanel());
        tabs.add("Goals", goalPanel());
        tabs.add("Subscriptions", subscriptionPanel());
        tabs.add("AI coach", coachPanel());
        add(tabs, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    // ---------------- FinancialView ----------------
    @Override public void display(String data) {
        lastMessage = data;
        SwingUtilities.invokeLater(() -> { status.setForeground(Color.BLACK); status.setText(data.split("\n")[0]); output.append(data + "\n\n"); scrollDown(); });
    }

    @Override public void displayError(String message) {
        SwingUtilities.invokeLater(() -> { status.setForeground(Color.RED); status.setText(message); output.append("ERROR: " + message + "\n\n"); scrollDown(); });
    }

    @Override public void showDashboard(MonthlySummary s) {
        SwingUtilities.invokeLater(() -> {
            status.setForeground(Color.BLACK);
            status.setText("Dashboard shown");
            output.append(DashboardFormatter.format(s) + "\n");
            chart.setData(s.getCategoryTotals());
            savingsBar.setValue((int) Math.round(Math.max(0, Math.min(1, s.getSavingsRate())) * 100));
            savingsBar.setString(String.format("Savings rate %.1f%%", s.getSavingsRate() * 100));
            scrollDown();
        });
    }

    /** Asks the user a question in a dialog (the last displayed message is the question). Safe to call from the worker thread. */
    @Override public String getUserInput() {
        AtomicReference<String> result = new AtomicReference<>("");
        Runnable ask = () -> { String r = JOptionPane.showInputDialog(this, lastMessage); result.set(r == null ? "" : r); };
        try {
            if (SwingUtilities.isEventDispatchThread()) ask.run(); else SwingUtilities.invokeAndWait(ask);
        } catch (InterruptedException | InvocationTargetException e) { Thread.currentThread().interrupt(); }
        return result.get();
    }

    /** Builds a FinancialEntry from the form data and hands it to the controller (class diagram: submitEntry). */
    public void submitEntry(FinancialEntry entry) {
        if (entry instanceof Income) controller.logIncome((Income) entry); else controller.logExpense((Expense) entry);
    }

    // ---------------- panels ----------------
    private JPanel logPanel() {
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        kind.addActionListener(e -> {
            boolean exp = kind.getSelectedItem().equals("Expense");
            label1.setText(exp ? "Category" : "Source");
            label2.setText(exp ? "Merchant" : "Type");
        });
        p.add(new JLabel("Kind")); p.add(kind);
        p.add(new JLabel("Amount")); p.add(amount);
        p.add(new JLabel("Date (YYYY-MM-DD)")); p.add(date);
        p.add(label1); p.add(field1);
        p.add(label2); p.add(field2);
        p.add(new JLabel("Description (optional)")); p.add(description);
        JButton save = new JButton("Save"); save.addActionListener(e -> save());
        JButton voice = new JButton("Voice entry"); voice.addActionListener(e -> async(controller::logVoiceEntry));
        p.add(save); p.add(voice);
        return p;
    }

    private void save() {
        double a;
        LocalDate d;
        try { a = Double.parseDouble(amount.getText().trim()); } catch (NumberFormatException ex) { displayError("The amount must be a number."); return; }
        try { d = LocalDate.parse(date.getText().trim()); } catch (RuntimeException ex) { displayError("The date must look like 2026-11-06."); return; }
        FinancialEntry entry = kind.getSelectedItem().equals("Expense")
                ? new Expense(a, d, description.getText(), field1.getText(), field2.getText())
                : new Income(a, d, description.getText(), field1.getText(), field2.getText());
        async(() -> submitEntry(entry));
    }

    private JPanel dashboardPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Month")); top.add(month); top.add(new JLabel("Year")); top.add(year);
        JButton show = new JButton("Dashboard");
        show.addActionListener(e -> { int m = (int) month.getValue(), y = (int) year.getValue(); async(() -> controller.requestDashboard(m, y)); });
        JButton summary = new JButton("End-of-month summary (AI)");
        summary.addActionListener(e -> { int m = (int) month.getValue(), y = (int) year.getValue(); async(() -> controller.requestMonthlySummary(m, y)); });
        top.add(show); top.add(summary);
        savingsBar.setStringPainted(true);
        savingsBar.setString("Savings rate");
        JPanel p = new JPanel(new BorderLayout());
        p.add(top, BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);
        p.add(savingsBar, BorderLayout.SOUTH);
        return p;
    }

    private JPanel goalPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField name = new JTextField(10), target = new JTextField(7), current = new JTextField(7),
                deadline = new JTextField(LocalDate.now().plusMonths(6).toString(), 9);
        JButton add = new JButton("Save goal");
        add.addActionListener(e -> {
            try {
                FinancialGoal g = new FinancialGoal(name.getText().trim(), Double.parseDouble(target.getText().trim()),
                        Double.parseDouble(current.getText().trim()), LocalDate.parse(deadline.getText().trim()));
                async(() -> controller.addGoal(g));
            } catch (RuntimeException ex) { displayError("Enter a name, numeric amounts and a deadline like 2027-05-01."); }
        });
        JButton summary = new JButton("Goal summary"); summary.addActionListener(e -> async(controller::requestGoalSummary));
        JButton advice = new JButton("Savings advice (AI)"); advice.addActionListener(e -> async(controller::requestSavingsAdvice));
        p.add(new JLabel("Name")); p.add(name); p.add(new JLabel("Target")); p.add(target);
        p.add(new JLabel("Saved")); p.add(current); p.add(new JLabel("Deadline")); p.add(deadline);
        p.add(add); p.add(summary); p.add(advice);
        return p;
    }

    private JPanel subscriptionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField name = new JTextField(10), amt = new JTextField(6), billing = new JTextField(LocalDate.now().toString(), 9), id = new JTextField(3);
        JComboBox<String> freq = new JComboBox<>(new String[]{"MONTHLY", "YEARLY", "WEEKLY"});
        JButton add = new JButton("Add");
        add.addActionListener(e -> {
            try {
                Subscription s = new Subscription(name.getText().trim(), Double.parseDouble(amt.getText().trim()), (String) freq.getSelectedItem(), LocalDate.parse(billing.getText().trim()));
                async(() -> controller.manageSubscription(s));
            } catch (RuntimeException ex) { displayError("Enter a name, a numeric amount and a date like 2026-11-06."); }
        });
        JButton remove = new JButton("Remove #");
        remove.addActionListener(e -> {
            try { int n = Integer.parseInt(id.getText().trim()); async(() -> controller.removeSubscription(n)); }
            catch (NumberFormatException ex) { displayError("Enter the subscription number to remove."); }
        });
        JButton list = new JButton("List"); list.addActionListener(e -> async(controller::requestSubscriptions));
        p.add(new JLabel("Name")); p.add(name); p.add(new JLabel("Amount")); p.add(amt); p.add(freq);
        p.add(new JLabel("Billing")); p.add(billing); p.add(add); p.add(id); p.add(remove); p.add(list);
        return p;
    }

    private JPanel coachPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ask = new JButton("Ask");
        Runnable send = () -> {
            String q = coachQuestion.getText().trim();
            if (q.isEmpty()) { displayError("Type a question first."); return; }
            coachQuestion.setText("");
            display("You: " + q);
            async(() -> controller.askFinancialCoach(q));
        };
        ask.addActionListener(e -> send.run());
        coachQuestion.addActionListener(e -> send.run());
        p.add(new JLabel("Question")); p.add(coachQuestion); p.add(ask);
        return p;
    }

    private void async(Runnable r) { worker.submit(r); }
    private void scrollDown() { output.setCaretPosition(output.getDocument().getLength()); }

    /** Simple bar chart of spending by category. */
    private static class ChartPanel extends JPanel {
        private Map<String, Double> data = Map.of();
        ChartPanel() { setPreferredSize(new Dimension(520, 160)); }
        void setData(Map<String, Double> d) { data = d; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) { g.drawString("Spending by category appears here.", 12, 20); return; }
            double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            int y = 10;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                if (y > getHeight() - 20) break;
                g.setColor(Color.BLACK);
                g.drawString(e.getKey(), 8, y + 12);
                g.setColor(new Color(70, 130, 180));
                g.fillRect(120, y, (int) (e.getValue() / max * (getWidth() - 240)), 14);
                g.setColor(Color.BLACK);
                g.drawString(String.format("$%.2f", e.getValue()), 125 + (int) (e.getValue() / max * (getWidth() - 240)), y + 12);
                y += 20;
            }
        }
    }
}

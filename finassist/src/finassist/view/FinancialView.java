package finassist.view;

import finassist.model.MonthlySummary;

/** MVC: View interface. GUIView and CLIView implement it. */
public interface FinancialView {
    void display(String data);
    void displayError(String message);
    void showDashboard(MonthlySummary summary);
    /** Asks the user for one line of input (used e.g. to confirm a voice entry). */
    String getUserInput();
}

package finassist.observer;

import java.util.ArrayList;
import java.util.List;

/** Observer pattern: Subject. FinancialService extends it. */
public class FinancialDataSubject {
    private final List<FinancialObserver> observers = new ArrayList<>();

    public void addObserver(FinancialObserver observer) {
        if (observer != null && !observers.contains(observer)) observers.add(observer);
    }

    public void removeObserver(FinancialObserver observer) { observers.remove(observer); }

    public void notifyObservers() {
        for (FinancialObserver o : new ArrayList<>(observers)) o.update();
    }
}

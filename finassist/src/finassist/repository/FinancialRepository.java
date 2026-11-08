package finassist.repository;

import finassist.exception.ValidationException;
import finassist.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Stores the user's data. Kept in memory and, when a file is given, saved to disk after every change. */
public class FinancialRepository {
    private User user = new User(1, "Default user");
    private int nextEntryId = 1, nextGoalId = 1, nextSubscriptionId = 1;
    private final Path file;

    public FinancialRepository() { this.file = null; }

    public FinancialRepository(Path file) {
        this.file = file;
        if (file != null && Files.exists(file)) load();
    }

    public synchronized void saveEntry(FinancialEntry entry) {
        if (entry.getEntryId() == 0) entry.setEntryId(nextEntryId++);
        user.getFinancialEntries().add(entry);
        persist();
    }

    public synchronized List<FinancialEntry> getEntries() { return new ArrayList<>(user.getFinancialEntries()); }

    public synchronized List<FinancialEntry> getEntriesByMonth(int month, int year) {
        List<FinancialEntry> out = new ArrayList<>();
        for (FinancialEntry e : user.getFinancialEntries()) {
            YearMonth ym = YearMonth.from(e.getDate());
            if (ym.getMonthValue() == month && ym.getYear() == year) out.add(e);
        }
        return out;
    }

    public synchronized void saveGoal(FinancialGoal goal) {
        if (goal.getGoalId() == 0) goal.setGoalId(nextGoalId++);
        user.getFinancialGoals().add(goal);
        persist();
    }

    public synchronized List<FinancialGoal> getGoals() { return new ArrayList<>(user.getFinancialGoals()); }

    public synchronized void saveSubscription(Subscription s) {
        if (s.getSubscriptionId() == 0) s.setSubscriptionId(nextSubscriptionId++);
        user.getSubscriptions().add(s);
        persist();
    }

    public synchronized List<Subscription> getSubscriptions() { return new ArrayList<>(user.getSubscriptions()); }

    public synchronized void removeSubscription(int id) {
        boolean removed = user.getSubscriptions().removeIf(s -> s.getSubscriptionId() == id);
        if (!removed) throw new ValidationException("Subscription " + id + " does not exist.");
        persist();
    }

    /** Writes the current data to disk (also called after an entry is changed in place). */
    public synchronized void persist() {
        if (file == null) return;
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(user);
            out.writeInt(nextEntryId); out.writeInt(nextGoalId); out.writeInt(nextSubscriptionId);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save data to " + file, e);
        }
    }

    private void load() {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            user = (User) in.readObject();
            nextEntryId = in.readInt(); nextGoalId = in.readInt(); nextSubscriptionId = in.readInt();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not read " + file + ", starting with empty data: " + e.getMessage());
        }
    }
}

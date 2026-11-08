package finassist.service;

import finassist.exception.ValidationException;
import finassist.model.Subscription;
import finassist.repository.FinancialRepository;

import java.util.List;
import java.util.Set;

/** Manages recurring expenses (F09). */
public class SubscriptionService {
    private static final Set<String> FREQUENCIES = Set.of("WEEKLY", "MONTHLY", "YEARLY");
    private final FinancialRepository repository;

    public SubscriptionService(FinancialRepository repository) { this.repository = repository; }

    public void addSubscription(Subscription s) {
        if (s.getName() == null || s.getName().isBlank()) throw new ValidationException("The subscription name is required.");
        if (s.getAmount() <= 0) throw new ValidationException("The subscription amount must be a positive number.");
        if (!FREQUENCIES.contains(s.getFrequency())) throw new ValidationException("The frequency must be WEEKLY, MONTHLY or YEARLY.");
        if (s.getBillingDate() == null) throw new ValidationException("The billing date is required.");
        repository.saveSubscription(s);
    }

    public void removeSubscription(int id) { repository.removeSubscription(id); }

    public List<Subscription> getSubscriptions() { return repository.getSubscriptions(); }

    public double calculateMonthlyCost() {
        return repository.getSubscriptions().stream().mapToDouble(Subscription::getMonthlyCost).sum();
    }
}

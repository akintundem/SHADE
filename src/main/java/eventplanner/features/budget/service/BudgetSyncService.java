package eventplanner.features.budget.service;

import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.repository.BudgetRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service for syncing budget data with other features (tickets, timeline, etc.)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BudgetSyncService {

    private final BudgetRepository budgetRepository;
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;

    /**
     * Sync ticket revenue with budget.
     * Called when tickets are sold or refunded.
     */
    public void syncTicketRevenue(UUID eventId) {
        budgetRepository.findByEventId(eventId).ifPresent(budget -> {
            // Calculate actual revenue from sold tickets
            BigDecimal actualRevenue = ticketRepository.sumActualRevenue(eventId);
            budget.setTotalRevenue(actualRevenue != null ? actualRevenue : BigDecimal.ZERO);

            // Calculate projected revenue from all available tickets
            BigDecimal projectedRevenue = ticketTypeRepository.sumProjectedRevenue(eventId);
            budget.setProjectedRevenue(projectedRevenue != null ? projectedRevenue : BigDecimal.ZERO);

            // Calculate net position (revenue - expenses)
            recalculateNetPosition(budget);

            budgetRepository.save(budget);
            log.info("Synced ticket revenue for event {}: actual={}, projected={}",
                    eventId, actualRevenue, projectedRevenue);
        });
    }

    /**
     * Recalculate budget totals and net position.
     * Call this after ticket sales or expense changes.
     */
    public void recalculateBudgetHealth(UUID eventId) {
        budgetRepository.findByEventId(eventId).ifPresent(budget -> {
            recalculateNetPosition(budget);
            updateBudgetStatus(budget);
            budgetRepository.save(budget);
        });
    }

    private void recalculateNetPosition(Budget budget) {
        BigDecimal revenue = budget.getTotalRevenue() != null ? budget.getTotalRevenue() : BigDecimal.ZERO;
        BigDecimal expenses = budget.getTotalActual() != null ? budget.getTotalActual() : BigDecimal.ZERO;
        budget.setNetPosition(revenue.subtract(expenses));
    }

    private void updateBudgetStatus(Budget budget) {
        if (budget.getTotalBudget() == null || budget.getTotalBudget().compareTo(BigDecimal.ZERO) == 0) {
            budget.setBudgetStatus("DRAFT");
            return;
        }

        BigDecimal totalActual = budget.getTotalActual() != null ? budget.getTotalActual() : BigDecimal.ZERO;
        BigDecimal totalBudget = budget.getTotalBudget();

        BigDecimal usedPercentage = totalActual
                .divide(totalBudget, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (usedPercentage.compareTo(new BigDecimal("90")) >= 0) {
            budget.setBudgetStatus("CRITICAL");
        } else if (usedPercentage.compareTo(new BigDecimal("75")) >= 0) {
            budget.setBudgetStatus("WARNING");
        } else if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
            budget.setBudgetStatus("ON_TRACK");
        } else {
            budget.setBudgetStatus("PLANNED");
        }
    }
}

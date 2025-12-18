package eventplanner.features.budget.service;

import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.entity.BudgetLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service to handle budget export operations (CSV, PDF)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Export budget and line items to CSV format
     */
    public byte[] exportToCSV(Budget budget) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            
            // Header section
            writer.println("Budget Export");
            writer.println("Event ID," + budget.getEventId());
            writer.println("Total Budget," + formatCurrency(budget.getTotalBudget(), budget.getCurrency()));
            writer.println("Currency," + budget.getCurrency());
            writer.println("Contingency," + budget.getContingencyPercentage() + "%");
            writer.println("Status," + budget.getBudgetStatus());
            writer.println("Created," + budget.getCreatedAt().format(DATE_FORMATTER));
            writer.println();
            
            // Line items section
            writer.println("Line Items");
            writer.println("Category,Description,Estimated Cost,Actual Cost,Quantity,Status,Priority,Notes");
            
            if (budget.getLineItems() != null && !budget.getLineItems().isEmpty()) {
                for (BudgetLineItem item : budget.getLineItems()) {
                    writer.printf("%s,%s,%s,%s,%d,%s,%s,%s%n",
                        escapeCSV(item.getCategory()),
                        escapeCSV(item.getDescription()),
                        formatAmount(item.getEstimatedCost()),
                        formatAmount(item.getActualCost()),
                        item.getQuantity() != null ? item.getQuantity() : 1,
                        item.getPlanningStatus() != null ? item.getPlanningStatus().toString() : "",
                        escapeCSV(item.getPriority()),
                        escapeCSV(item.getNotes())
                    );
                }
            }
            
            writer.flush();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to export budget to CSV", e);
            throw new RuntimeException("Failed to export budget to CSV", e);
        }
    }
    
    /**
     * Generate CSV template for budget import with standardized categories
     */
    public byte[] generateImportTemplate() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
            
            writer.println("# Budget Import Template");
            writer.println("# Fill in the budget details below");
            writer.println("# Line 1: Currency (e.g., USD, EUR, GBP)");
            writer.println("# Line 2: Contingency Percentage (e.g., 10)");
            writer.println("# Line 3+: Category, Description, Estimated Cost, Actual Cost, Quantity, Priority, Notes");
            writer.println();
            writer.println("Currency,USD");
            writer.println("Contingency,10");
            writer.println();
            writer.println("# Standardized Categories:");
            writer.println("# - Venue & Facilities");
            writer.println("# - Catering & Food");
            writer.println("# - Technical Equipment");
            writer.println("# - Marketing & Promotion");
            writer.println("# - Entertainment");
            writer.println("# - Staffing & Personnel");
            writer.println("# - Decor & Ambiance");
            writer.println("# - Transportation & Logistics");
            writer.println("# - Miscellaneous");
            writer.println();
            writer.println("Category,Description,Estimated Cost,Actual Cost,Quantity,Priority,Notes");
            writer.println("Venue & Facilities,Conference hall rental,10000,0,1,HIGH,Main venue");
            writer.println("Catering & Food,Lunch for 100 attendees,5000,0,100,HIGH,");
            writer.println("Technical Equipment,AV equipment rental,3000,0,1,MEDIUM,Projector and sound system");
            
            writer.flush();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate import template", e);
            throw new RuntimeException("Failed to generate import template", e);
        }
    }
    
    /**
     * Get list of standardized budget categories
     */
    public List<String> getStandardizedCategories() {
        return List.of(
            "Venue & Facilities",
            "Catering & Food",
            "Technical Equipment",
            "Marketing & Promotion",
            "Entertainment",
            "Staffing & Personnel",
            "Decor & Ambiance",
            "Transportation & Logistics",
            "Miscellaneous"
        );
    }
    
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toString() : "0.00";
    }
    
    private String formatCurrency(BigDecimal amount, String currency) {
        return String.format("%s %s", currency != null ? currency : "USD", formatAmount(amount));
    }
}


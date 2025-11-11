package eventplanner.features.budget.service;

import eventplanner.features.budget.dto.BudgetLineItemCreateRequest;
import eventplanner.features.budget.dto.request.BulkLineItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service to handle budget import operations from CSV
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetImportService {
    
    /**
     * Import budget line items from CSV file
     * Expected format: Category,Description,Estimated Cost,Actual Cost,Quantity,Priority,Notes
     */
    public BulkLineItemRequest importFromCSV(MultipartFile file, UUID budgetId) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            List<BudgetLineItemCreateRequest> lineItems = new ArrayList<>();
            String line;
            boolean inDataSection = false;
            
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                // Parse metadata (currently unused but available for future use)
                if (line.startsWith("Currency,")) {
                    // String currency = line.split(",")[1].trim();
                    continue;
                }
                
                // Skip header row
                if (line.startsWith("Category,Description")) {
                    inDataSection = true;
                    continue;
                }
                
                // Parse data rows
                if (inDataSection) {
                    BudgetLineItemCreateRequest item = parseLineItem(line);
                    if (item != null) {
                        lineItems.add(item);
                    }
                }
            }
            
            if (lineItems.isEmpty()) {
                throw new IllegalArgumentException("No line items found in import file");
            }
            
            BulkLineItemRequest request = new BulkLineItemRequest();
            request.setBudgetId(budgetId);
            request.setLineItems(lineItems);
            
            log.info("Successfully imported {} line items from CSV", lineItems.size());
            return request;
            
        } catch (Exception e) {
            log.error("Failed to import budget from CSV", e);
            throw new RuntimeException("Failed to import budget from CSV: " + e.getMessage(), e);
        }
    }
    
    private BudgetLineItemCreateRequest parseLineItem(String line) {
        try {
            String[] parts = parseCSVLine(line);
            if (parts.length < 3) {
                return null; // Invalid row
            }
            
            BudgetLineItemCreateRequest item = new BudgetLineItemCreateRequest();
            item.setCategory(parts[0].trim());
            item.setDescription(parts.length > 1 ? parts[1].trim() : "");
            
            // Parse estimated cost
            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                try {
                    item.setEstimatedCost(new BigDecimal(parts[2].trim()));
                } catch (NumberFormatException e) {
                    item.setEstimatedCost(BigDecimal.ZERO);
                }
            }
            
            // Parse actual cost
            if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                try {
                    item.setActualCost(new BigDecimal(parts[3].trim()));
                } catch (NumberFormatException e) {
                    item.setActualCost(BigDecimal.ZERO);
                }
            }
            
            // Parse quantity
            if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                try {
                    item.setQuantity(Integer.parseInt(parts[4].trim()));
                } catch (NumberFormatException e) {
                    item.setQuantity(1);
                }
            }
            
            return item;
            
        } catch (Exception e) {
            log.warn("Failed to parse line: {}", line, e);
            return null;
        }
    }
    
    /**
     * Parse CSV line handling quoted fields
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result.toArray(new String[0]);
    }
}


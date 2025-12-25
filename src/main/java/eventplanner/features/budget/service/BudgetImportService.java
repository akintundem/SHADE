package eventplanner.features.budget.service;

import eventplanner.features.budget.dto.request.BudgetLineItemAutoSaveRequest;
import eventplanner.features.budget.dto.request.BulkLineItemRequest;
import lombok.RequiredArgsConstructor;
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
public class BudgetImportService {
    
    /**
     * Import budget line items from CSV file
     * Expected format: Category,Description,Estimated Cost,Actual Cost,Quantity,Priority,Notes
     */
    public BulkLineItemRequest importFromCSV(MultipartFile file, UUID budgetId) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            List<BudgetLineItemAutoSaveRequest> lineItems = new ArrayList<>();
            String line;
            boolean inDataSection = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                if (line.startsWith("Currency,")) {
                    continue;
                }
                
                if (line.startsWith("Category,Description")) {
                    inDataSection = true;
                    continue;
                }
                
                if (inDataSection) {
                    BudgetLineItemAutoSaveRequest item = parseLineItem(line);
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
            
            return request;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import budget from CSV. Please check the file format.", e);
        }
    }
    
    private BudgetLineItemAutoSaveRequest parseLineItem(String line) {
        try {
            String[] parts = parseCSVLine(line);
            if (parts.length < 3) {
                return null;
            }
            
            BudgetLineItemAutoSaveRequest item = new BudgetLineItemAutoSaveRequest();
            item.setCategoryName(parts[0].trim());
            item.setDescription(parts.length > 1 ? parts[1].trim() : "");
            
            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                try {
                    item.setEstimatedCost(new BigDecimal(parts[2].trim()));
                } catch (NumberFormatException e) {
                    item.setEstimatedCost(BigDecimal.ZERO);
                }
            }
            
            if (parts.length > 3 && !parts[3].trim().isEmpty()) {
                try {
                    item.setActualCost(new BigDecimal(parts[3].trim()));
                } catch (NumberFormatException e) {
                    item.setActualCost(BigDecimal.ZERO);
                }
            }
            
            if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                try {
                    item.setQuantity(Integer.parseInt(parts[4].trim()));
                } catch (NumberFormatException e) {
                    item.setQuantity(1);
                }
            }
            
            return item;
            
        } catch (Exception e) {
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

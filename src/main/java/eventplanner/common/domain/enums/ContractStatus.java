package eventplanner.common.domain.enums;

/**
 * Contract status enumeration for vendor relationships
 */
public enum ContractStatus {
    DRAFT,          // Contract in draft
    SENT,           // Contract sent to vendor
    REVIEWED,       // Contract under review
    SIGNED,         // Contract signed
    EXECUTED,       // Contract executed
    EXPIRED,        // Contract expired
    TERMINATED      // Contract terminated
}

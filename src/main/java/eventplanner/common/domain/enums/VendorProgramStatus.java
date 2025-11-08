package eventplanner.common.domain.enums;

/**
 * Status for a vendor's participation in the Shade platform vendor program.
 */
public enum VendorProgramStatus {
    PENDING,        // Awaiting review/approval
    APPROVED,       // Approved and visible as prioritized vendor
    SUSPENDED,      // Temporarily suspended by platform
    REJECTED        // Not accepted into the program
}


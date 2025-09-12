package com.agentic.e2etester.integration.database;

import java.util.Collections;
import java.util.List;

/**
 * Result of comparing two documents.
 */
public class DocumentComparisonResult {
    
    private final boolean identical;
    private final List<FieldDifference> differences;
    private final String errorMessage;
    
    private DocumentComparisonResult(boolean identical, List<FieldDifference> differences, String errorMessage) {
        this.identical = identical;
        this.differences = differences != null ? differences : Collections.emptyList();
        this.errorMessage = errorMessage;
    }
    
    public static DocumentComparisonResult identical() {
        return new DocumentComparisonResult(true, null, null);
    }
    
    public static DocumentComparisonResult different(List<FieldDifference> differences) {
        return new DocumentComparisonResult(false, differences, null);
    }
    
    public static DocumentComparisonResult failure(String errorMessage) {
        return new DocumentComparisonResult(false, null, errorMessage);
    }
    
    public boolean isIdentical() {
        return identical;
    }
    
    public boolean isSuccess() {
        return errorMessage == null;
    }
    
    public List<FieldDifference> getDifferences() {
        return differences;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getDifferenceCount() {
        return differences.size();
    }
    
    @Override
    public String toString() {
        return "DocumentComparisonResult{" +
                "identical=" + identical +
                ", differenceCount=" + differences.size() +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
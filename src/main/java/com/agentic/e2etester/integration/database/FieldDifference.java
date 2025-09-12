package com.agentic.e2etester.integration.database;

/**
 * Represents a difference between two document fields.
 */
public class FieldDifference {
    
    private final String fieldName;
    private final Object value1;
    private final Object value2;
    
    public FieldDifference(String fieldName, Object value1, Object value2) {
        this.fieldName = fieldName;
        this.value1 = value1;
        this.value2 = value2;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getValue1() {
        return value1;
    }
    
    public Object getValue2() {
        return value2;
    }
    
    @Override
    public String toString() {
        return "FieldDifference{" +
                "fieldName='" + fieldName + '\'' +
                ", value1=" + value1 +
                ", value2=" + value2 +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FieldDifference that = (FieldDifference) o;
        
        if (!fieldName.equals(that.fieldName)) return false;
        if (value1 != null ? !value1.equals(that.value1) : that.value1 != null) return false;
        return value2 != null ? value2.equals(that.value2) : that.value2 == null;
    }
    
    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + (value1 != null ? value1.hashCode() : 0);
        result = 31 * result + (value2 != null ? value2.hashCode() : 0);
        return result;
    }
}
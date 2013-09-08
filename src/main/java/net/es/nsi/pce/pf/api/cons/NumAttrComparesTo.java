package net.es.nsi.pce.pf.api.cons;

/**
 * Defines a basic PCE constraint modeling an number attribute with a name,
 * Long value, and a comparison.
 * 
 * @author hacksaw
 */
public abstract class NumAttrComparesTo extends NumAttrConstraint {
    public enum ComparisonType {
        GREATER_THAN,
        LESS_THAN,
        EQUALS
    }
    
    private ComparisonType comparisonType;

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(ComparisonType comparisonType) {
        this.comparisonType = comparisonType;
    }
}

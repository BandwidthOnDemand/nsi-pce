package net.es.nsi.pce.pf.api.cons;

public class NumAttrComparesTo extends NumAttrConstraint {
    public enum ComparisonType {
        GREATER_THAN,
        LESS_THAN,
        EQUALS
    }
    private ComparisonType comparisonType;

    private Long val;

    public Long getVal() {
        return val;
    }

    public void setVal(Long val) {
        this.val = val;
    }

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(ComparisonType comparisonType) {
        this.comparisonType = comparisonType;
    }
}

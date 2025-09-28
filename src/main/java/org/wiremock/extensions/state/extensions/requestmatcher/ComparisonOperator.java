package org.wiremock.extensions.state.extensions.requestmatcher;

/**
 * Comparison operators for numeric and comparable value comparisons.
 */
public enum ComparisonOperator {
    eq,
    ne,
    lt,
    lte,
    gt,
    gte;

    /**
     * Compares two numeric values using this operator.
     * Converts numbers to Double for comparison to handle different numeric types.
     *
     * @param a first number to compare
     * @param b second number to compare
     * @return true if the comparison condition is satisfied
     * @throws IllegalArgumentException if null values cannot be compared with this operator
     */
    public <T extends Number> boolean compare(T a, T b) {

        if (a == null && b == null) {
            if (this == eq) return true;
            if (this == ne) return false;
        }

        if (a == null || b == null) {
            if (this == eq) return false;
            if (this == ne) return true;
            throw new IllegalArgumentException(
                String.format("Cannot compare %s with %s using operator %s", a, b, this)
            );
        }

        double valueA = a.doubleValue();
        double valueB = b.doubleValue();

        switch (this) {
            case eq:
                return valueA == valueB;
            case ne:
                return valueA != valueB;
            case lt:
                return valueA < valueB;
            case lte:
                return valueA <= valueB;
            case gt:
                return valueA > valueB;
            case gte:
                return valueA >= valueB;
            default:
                throw new IllegalStateException("Unsupported operator: " + this);
        }
    }

    /**
     * Returns the ComparisonOperator for the given name.
     * Name matching is case-insensitive (e.g., "EQ", "eq", "Eq" all work).
     *
     * @param name the operator name (eq, ne, lt, lte, gt, gte)
     * @return the corresponding ComparisonOperator
     * @throws IllegalArgumentException if the name does not match any operator
     */
    public static ComparisonOperator fromName(String name) {
        try {
            return valueOf(name.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operator name: " + name);
        }
    }
}
package org.wiremock.extensions.state.extensions.requestmatcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-tests for {@link ComparisonOperator}
 */
@DisplayName("ComparisonOperator Unit Tests")
class ComparisonOperatorTest {

    @Nested
    @DisplayName("Tests for compare() method with numeric values")
    class CompareTests {

        @DisplayName("eq operator: should return true if numbers are equal, false otherwise")
        @ParameterizedTest
        @CsvSource({
            "20, 10, false",
            "10, 10, true",
            "10, 20, false"
        })
        void test_eq(int a, int b, boolean result) {
            assertEquals(result, ComparisonOperator.eq.compare(a, b));
        }

        @DisplayName("ne operator: should return true if numbers are not equal, false otherwise")
        @ParameterizedTest
        @CsvSource({
            "20, 10, true",
            "10, 10, false",
            "10, 20, true"
        })
        void test_ne(int a, int b, boolean result) {
            assertEquals(result, ComparisonOperator.ne.compare(a, b));
        }

        @DisplayName("lt operator: should return true if first number is less than second")
        @ParameterizedTest
        @CsvSource({
            "20, 10, false",
            "10, 10, false",
            "10, 20, true"
        })
        void test_lt(int a, int b, boolean result) {
            assertEquals(result, ComparisonOperator.lt.compare(a, b));
        }

        @DisplayName("lte operator: should return true if first number is less than or equal to second")
        @ParameterizedTest
        @CsvSource({
            "10, 10, true",
            "5, 10, true",
            "10, 5, false"
        })
        void test_lte(int a, int b, boolean result) {
            assertEquals(result, ComparisonOperator.lte.compare(a, b));
        }

        @DisplayName("gt operator: should return true if first number is greater than second")
        @ParameterizedTest
        @CsvSource({
            "10, 10, false",
            "5, 10, false",
            "10, 5, true"
        })
        void test_gt(int a, int b, boolean result) {
            assertEquals(result, ComparisonOperator.gt.compare(a, b));
        }

        @DisplayName("gte operator: should return true if first number is greater than or equal to second")
        @ParameterizedTest
        @CsvSource({
            "10, 10, true",
            "5, 10, false",
            "10, 5, true"
        })
        void test_gte(int a, int b, boolean result) {
            assertEquals(result, ComparisonOperator.gte.compare(a, b));
        }
    }

    @Nested
    @DisplayName("Tests for fromName() method")
    class FromNameTests {

        @DisplayName("should return eq operator for lowercase name")
        @Test
        void test_lowerCase() {
            assertEquals(ComparisonOperator.eq, ComparisonOperator.fromName("eq"));
        }

        @DisplayName("should return gt operator for uppercase name")
        @Test
        void test_upperCase() {
            assertEquals(ComparisonOperator.gt, ComparisonOperator.fromName("GT"));
        }

        @DisplayName("should return lte operator for mixed case name")
        @Test
        void test_mixedCase() {
            assertEquals(ComparisonOperator.lte, ComparisonOperator.fromName("LtE"));
        }

        @DisplayName("should throw IllegalArgumentException for unknown operator name")
        @Test
        void test_unknownName() {
            assertThrows(IllegalArgumentException.class,
                () -> ComparisonOperator.fromName("xxx"));
        }
    }

    @DisplayName("Tests for handling null values with eq and ne operators")
    @ParameterizedTest(name = "{0}.compare({1}, {2}) should be {3}")
    @MethodSource("validNullCases")
    void test_null_handling(ComparisonOperator operator, Integer a, Integer b, Boolean result) {
        assertEquals(result, operator.compare(a, b));
    }

    @DisplayName("Tests for handling null values with lt, lte, gt, gte operators (should throw IllegalArgumentException)")
    @ParameterizedTest(name = "{0}.compare({1}, {2}) should throw IllegalArgumentException")
    @MethodSource("exceptionNullCases")
    void test_null_throwIllegalArgumentException(ComparisonOperator operator, Integer a, Integer b) {
        assertThrows(IllegalArgumentException.class,
            () -> operator.compare(a, b),
            String.format("Cannot compare %s with %s using operator %s", a, b, operator)
        );
    }

    private static Stream<Arguments> validNullCases() {
        return Stream.of(
            // both null
            Arguments.of(ComparisonOperator.eq, null, null, true),
            Arguments.of(ComparisonOperator.ne, null, null, false),
            // left null
            Arguments.of(ComparisonOperator.eq, null, 5, false),
            Arguments.of(ComparisonOperator.ne, null, 5, true),
            // right null
            Arguments.of(ComparisonOperator.eq, 5, null, false),
            Arguments.of(ComparisonOperator.ne, 5, null, true)
        );
    }

    private static Stream<Arguments> exceptionNullCases() {
        return Stream.of(
            // both null
            Arguments.of(ComparisonOperator.lt, null, null),
            Arguments.of(ComparisonOperator.lte, null, null),
            Arguments.of(ComparisonOperator.gt, null, null),
            Arguments.of(ComparisonOperator.gte, null, null),
            // left null
            Arguments.of(ComparisonOperator.lt, null, 5),
            Arguments.of(ComparisonOperator.lte, null, 5),
            Arguments.of(ComparisonOperator.gt, null, 5),
            Arguments.of(ComparisonOperator.gte, null, 5),
            // right null
            Arguments.of(ComparisonOperator.lt, 5, null),
            Arguments.of(ComparisonOperator.lte, 5, null),
            Arguments.of(ComparisonOperator.gt, 5, null),
            Arguments.of(ComparisonOperator.gte, 5, null)
        );
    }
}

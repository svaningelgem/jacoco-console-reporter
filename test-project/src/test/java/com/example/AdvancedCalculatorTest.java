package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedCalculatorTest {
    @Test
    void testMultiply() {
        AdvancedCalculator advCalc = new AdvancedCalculator();
        assertEquals(6, advCalc.multiply(2, 3));
    }

    @Test
    void testDivide() {
        AdvancedCalculator advCalc = new AdvancedCalculator();
        assertEquals(2, advCalc.divide(4, 2));
        assertThrows(IllegalArgumentException.class, () -> advCalc.divide(1, 0));
    }

    @Test
    void testCategorizeNumber() {
        AdvancedCalculator advCalc = new AdvancedCalculator();
        assertEquals("Positive", advCalc.categorizeNumber(5));
        assertEquals("Negative", advCalc.categorizeNumber(-3));
        assertEquals("Zero", advCalc.categorizeNumber(0));
    }
}
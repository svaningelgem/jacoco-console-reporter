package com.example;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdvancedCalculatorTest {
    @Test
    public void testMultiply() {
        AdvancedCalculator advCalc = new AdvancedCalculator();
        assertEquals(6, advCalc.multiply(2, 3));
    }

    @Test
    public void testDivide() {
        AdvancedCalculator advCalc = new AdvancedCalculator();
        assertEquals(2, advCalc.divide(4, 2));
        try {
            advCalc.divide(1, 0);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testCategorizeNumber() {
        AdvancedCalculator advCalc = new AdvancedCalculator();
        assertEquals("Positive", advCalc.categorizeNumber(5));
        assertEquals("Negative", advCalc.categorizeNumber(-3));
        assertEquals("Zero", advCalc.categorizeNumber(0));
    }
}
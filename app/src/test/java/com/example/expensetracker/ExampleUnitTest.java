package com.example.expensetracker;

import org.junit.Test;
import com.example.expensetracker.utils.OCRUtils;

import static org.junit.Assert.*;

public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testOcrAmountExtractionWithSymbol() {
        String receiptText = "Starbucks Coffee\nTotal Amount: ₹1,250.50\nThank you!";
        Double amount = OCRUtils.extractAmount(receiptText);
        assertNotNull(amount);
        assertEquals(1250.50, amount, 0.001);
    }

    @Test
    public void testOcrAmountExtractionWithRs() {
        String receiptText = "Supermarket\nItems: 5\nGrand Total: Rs. 850\nPaid via UPI";
        Double amount = OCRUtils.extractAmount(receiptText);
        assertNotNull(amount);
        assertEquals(850.0, amount, 0.001);
    }

    @Test
    public void testOcrAmountExtractionWithINR() {
        String receiptText = "Uber Ride\nTotal INR 499.00";
        Double amount = OCRUtils.extractAmount(receiptText);
        assertNotNull(amount);
        assertEquals(499.0, amount, 0.001);
    }

    @Test
    public void testOcrDescriptionExtraction() {
        String receiptText = "Description: Dinner at Restaurant\nAmount: ₹1200";
        String description = OCRUtils.extractDescription(receiptText);
        assertEquals("Dinner at Restaurant", description);
    }
}

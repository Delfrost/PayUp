package com.payup.app

import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import com.payup.app.service.ScreenshotParser
import org.junit.Assert.*
import org.junit.Test

class ScreenshotParserTest {

    @Test
    fun testSingleReceiptOcrParsing() {
        val ocrText = """
            Google Pay
            Paid to Blue Tokai Coffee
            ₹350.00
            Completed
            28 Oct 2026, 04:15 pm
            UPI transaction ID 428910293847
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(1, candidates.size)

        val candidate = candidates[0]
        assertEquals(35000L, candidate.amountPaise) // ₹350 = 35000 paise
        assertEquals("Blue Tokai Coffee", candidate.merchant)
        assertEquals(TransactionType.EXPENSE, candidate.type)
        assertEquals(TransactionStatus.COMPLETED, candidate.status)
        assertEquals("428910293847", candidate.referenceId)
        assertFalse(candidate.isExcluded)
    }

    @Test
    fun testMultiTransactionStatementParsing() {
        val ocrText = """
            Transaction History
            
            Swiggy
            ₹420.00
            Paid on 27 Oct
            
            Uber India
            ₹185.00
            Paid on 27 Oct
            
            Salary Deposit
            ₹25,000.00
            Received from TechCorp
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(3, candidates.size)

        val swiggy = candidates[0]
        assertEquals(42000L, swiggy.amountPaise)
        assertEquals("Swiggy", swiggy.merchant)
        assertEquals(TransactionType.EXPENSE, swiggy.type)

        val uber = candidates[1]
        assertEquals(18500L, uber.amountPaise)
        assertEquals("Uber India", uber.merchant)
        assertEquals(TransactionType.EXPENSE, uber.type)

        val salary = candidates[2]
        assertEquals(2500000L, salary.amountPaise)
        assertEquals(TransactionType.INCOME, salary.type)
    }

    @Test
    fun testFailedScreenshotPaymentExcludedByDefault() {
        val ocrText = """
            Payment Failed
            Paid to Decathlon
            ₹2,499.00
            Bank server declined transaction
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(1, candidates.size)
        val candidate = candidates[0]
        assertEquals(249900L, candidate.amountPaise)
        assertEquals(TransactionStatus.FAILED, candidate.status)
        assertTrue("Failed payment candidate must be excluded by default", candidate.isExcluded)
    }
}

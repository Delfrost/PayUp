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
        assertEquals("Google Pay", candidate.sourceApp)
        assertFalse(candidate.isExcluded)
    }

    @Test
    fun testGooglePaySingleReceiptDetailed() {
        val ocrText = """
            Google Pay
            Paid to
            Chaayos
            chaayos@icici
            ₹240
            Completed • Oct 29, 2026, 9:15 AM
            UPI transaction ID: 430319485721
            To: Chaayos
            From: State Bank of India •••• 1234
            Google transaction ID: CICAgPD1234
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(1, candidates.size)

        val candidate = candidates[0]
        assertEquals(24000L, candidate.amountPaise) // ₹240 = 24000 paise
        assertEquals("Chaayos", candidate.merchant)
        assertEquals(TransactionType.EXPENSE, candidate.type)
        assertEquals(TransactionStatus.COMPLETED, candidate.status)
        assertEquals("430319485721", candidate.referenceId)
        assertEquals("Google Pay", candidate.sourceApp)
        assertFalse(candidate.isExcluded)
    }

    @Test
    fun testGooglePayMultiHistoryStatement() {
        val ocrText = """
            Transaction history
            October 2026
            Paid to Starbucks Coffee
            28 Oct, 4:30 pm
            ₹350
            
            Received from Rahul
            27 Oct, 11:15 am
            +₹1,200
            
            Paid to Zomato
            26 Oct, 8:40 pm
            ₹482
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(3, candidates.size)

        val starbucks = candidates[0]
        assertEquals(35000L, starbucks.amountPaise)
        assertEquals("Starbucks Coffee", starbucks.merchant)
        assertEquals(TransactionType.EXPENSE, starbucks.type)
        assertEquals("Google Pay", starbucks.sourceApp)

        val rahul = candidates[1]
        assertEquals(120000L, rahul.amountPaise)
        assertEquals("Rahul", rahul.merchant)
        assertEquals(TransactionType.INCOME, rahul.type)

        val zomato = candidates[2]
        assertEquals(48200L, zomato.amountPaise)
        assertEquals("Zomato", zomato.merchant)
        assertEquals(TransactionType.EXPENSE, zomato.type)
    }

    @Test
    fun testSuperMoneySingleReceiptWithCashbackExclusion() {
        val ocrText = """
            super.money
            Payment Successful
            Paid to
            Blinkit
            ₹629
            superCashback
            ₹15 won!
            UPI Reference: 430491029384
            Debited from Axis Bank •••• 9876
            Date: 29 Oct 2026, 10:30 AM
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(1, candidates.size)

        val candidate = candidates[0]
        assertEquals(62900L, candidate.amountPaise)
        assertEquals("Blinkit", candidate.merchant)
        assertEquals(TransactionType.EXPENSE, candidate.type)
        assertEquals(TransactionStatus.COMPLETED, candidate.status)
        assertEquals("430491029384", candidate.referenceId)
        assertEquals("super.money", candidate.sourceApp)
        assertFalse(candidate.isExcluded)
    }

    @Test
    fun testSuperMoneyMultiTransactionHistory() {
        val ocrText = """
            Transactions
            Flipkart
            Debited from superCard
            - ₹1,499
            Success
            29 Oct 2026
            
            Cashback
            Credited to Wallet
            + ₹25
            Success
            28 Oct 2026
            
            McDonald's
            Debited from HDFC Bank
            - ₹280
            Pending
            28 Oct 2026
        """.trimIndent()

        val candidates = ScreenshotParser.parseText(ocrText)
        assertEquals(3, candidates.size)

        val flipkart = candidates[0]
        assertEquals(149900L, flipkart.amountPaise)
        assertEquals("Flipkart", flipkart.merchant)
        assertEquals(TransactionType.EXPENSE, flipkart.type)
        assertEquals(TransactionStatus.COMPLETED, flipkart.status)
        assertEquals("super.money", flipkart.sourceApp)

        val cashback = candidates[1]
        assertEquals(2500L, cashback.amountPaise)
        assertEquals(TransactionType.INCOME, cashback.type)

        val mcd = candidates[2]
        assertEquals(28000L, mcd.amountPaise)
        assertEquals("McDonald's", mcd.merchant)
        assertEquals(TransactionStatus.PENDING, mcd.status)
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

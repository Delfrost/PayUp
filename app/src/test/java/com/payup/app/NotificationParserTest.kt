package com.payup.app

import com.payup.app.data.model.TransactionStatus
import com.payup.app.data.model.TransactionType
import com.payup.app.service.NotificationParser
import org.junit.Assert.*
import org.junit.Test

class NotificationParserTest {

    @Test
    fun testGooglePayPaymentDetection() {
        val title = "Payment to Sharma Sweets"
        val text = "You paid ₹150.00 to Sharma Sweets via UPI. UPI Ref: 432198765432"
        val pkg = "com.google.android.apps.nbu.paisa.user"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals(15000L, parsed!!.amountPaise)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(TransactionStatus.COMPLETED, parsed.status)
        assertEquals("Google Pay", parsed.sourceApp)
        assertEquals("432198765432", parsed.referenceId)
    }

    @Test
    fun testSuperMoneyPaymentDetection() {
        val title = "Paid ₹10 via super.money"
        val text = "Paid to Metro Station via super.money. UTR 998877665544"
        val pkg = "money.super.payments"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals(1000L, parsed!!.amountPaise) // ₹10 = 1000 paise
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("super.money", parsed.sourceApp)
        assertEquals("998877665544", parsed.referenceId)
    }

    @Test
    fun testFlashSaleCashbackPromoNotificationRejected() {
        // User's exact reported bug
        val title = "🚨 Flash sale ending soon 39:47"
        val text = "👟 Get ₹1,000 cashback on Sneakers. Shop Now!"
        val pkg = "money.super.payments"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNull("Marketing promo must NEVER be recorded as an expense or transaction", parsed)
    }

    @Test
    fun testDiscountAndRechargePromoRejected() {
        val title = "Special offer for you"
        val text = "Flat ₹50 off on electricity bill. Recharge now using code SAVE50."
        val pkg = "com.phonepe.app"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNull("Coupon/discount offers must be rejected", parsed)
    }

    @Test
    fun testRealCashbackCreditedIsIncome() {
        val title = "Cashback Credited!"
        val text = "Cashback of ₹50.00 credited to your super.money wallet for recent UPI payment."
        val pkg = "money.super.payments"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals(5000L, parsed!!.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type) // INCOME, never expense!
        assertEquals("super.money Cashback", parsed.merchant)
    }

    @Test
    fun testMicroPayment1RupeeTrackedWithoutThreshold() {
        val title = "Paid ₹1"
        val text = "You paid ₹1.00 to Local Chaiwala on Google Pay"
        val pkg = "com.google.android.apps.nbu.paisa.user"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals(100L, parsed!!.amountPaise) // exactly 100 paise
    }

    @Test
    fun testCreditIncomeDetection() {
        val title = "Money received"
        val text = "You received ₹50,000 from Employer Corp via UPI Ref 112233445566"
        val pkg = "net.one97.paytm"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNotNull(parsed)
        assertEquals(5000000L, parsed!!.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("Paytm", parsed.sourceApp)
    }

    @Test
    fun testFailedTransactionIgnored() {
        val title = "Payment Failed"
        val text = "Payment of ₹500 to Reliance Fresh failed. Amount will not be deducted."
        val pkg = "com.google.android.apps.nbu.paisa.user"

        val parsed = NotificationParser.parse(pkg, title, text)
        assertNull(parsed)
    }

    @Test
    fun testMissingAmountCreatesPendingObservationNeverZeroExpense() {
        // Notification with debit indicator and ref, but missing amount
        val title = "Payment processing"
        val text = "Paid to Apollo Pharmacy. Transaction in progress. UPI Ref 554433221100"
        val pkg = "com.google.android.apps.nbu.paisa.user"

        val result = NotificationParser.parseWithObservation(pkg, title, text)
        assertNotNull(result)
        assertTrue("Must be an Incomplete observation when amount is missing", result is NotificationParser.ParseResult.Incomplete)

        val obs = (result as NotificationParser.ParseResult.Incomplete).observation
        assertNull("Amount must be null, never guessed or fabricated as ₹0", obs.amountPaise)
        assertEquals("Apollo Pharmacy", obs.merchant)
        assertEquals("554433221100", obs.referenceId)
        assertEquals(com.payup.app.data.model.ObservationStatus.PENDING_REVIEW, obs.status)

        // Also verify standard parse() returns null (never a ₹0 transaction)
        val standardParsed = NotificationParser.parse(pkg, title, text)
        assertNull("Standard transaction parser must return null for missing amount", standardParsed)
    }
}

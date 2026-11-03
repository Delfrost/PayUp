package com.payup.app

import com.payup.app.data.model.TransactionType
import com.payup.app.service.SmsParser
import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun testHdfcDebitSms() {
        val sender = "VM-HDFCBK"
        val body = "Rs 150.00 debited from A/c **4321 on 01-11-26 to VPA Swiggy@icici (UPI Ref 432198765432). Avl bal Rs 5430.00"

        val parsed = SmsParser.parse(sender, body)
        assertNotNull(parsed)
        assertEquals(15000L, parsed!!.amountPaise)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("HDFC Bank", parsed.bankName)
        assertEquals("432198765432", parsed.referenceId)
    }

    @Test
    fun testSbiCreditSms() {
        val sender = "AD-SBIINB"
        val body = "Dear Customer, Your A/c XXXX1234 Credited by Rs 25,000.00 on 01Nov26 by Transfer. Ref 987654321012. Bal Rs 45,000"

        val parsed = SmsParser.parse(sender, body)
        assertNotNull(parsed)
        assertEquals(2500000L, parsed!!.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("State Bank of India", parsed.bankName)
    }

    @Test
    fun testOtpSmsIgnored() {
        val sender = "AX-HDFCBK"
        val body = "123456 is your secret OTP for NetBanking login. Do not share with anyone."

        val parsed = SmsParser.parse(sender, body)
        assertNull(parsed)
    }

    @Test
    fun testCreditWithSentByClauseIsIncome() {
        val sender = "VM-HDFCBK"
        val body = "Dear Customer, A/c *4321 credited by Rs 2,500.00 on 02-Nov-26 sent by Rahul Sharma via UPI. Ref 123456789012."

        val parsed = SmsParser.parse(sender, body)
        assertNotNull(parsed)
        assertEquals(250000L, parsed!!.amountPaise)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("123456789012", parsed.referenceId)
    }

    @Test
    fun testQrCodePaymentNotDropped() {
        val sender = "VM-HDFCBK"
        val body = "Rs 150.00 debited from A/c **4321 on 01-11-26 via QR code to Chai Point. UPI Ref 432198765432."

        val parsed = SmsParser.parse(sender, body)
        assertNotNull("Payment with 'QR code' must not be rejected as promo", parsed)
        assertEquals(15000L, parsed!!.amountPaise)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }
}

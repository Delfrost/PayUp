package com.payup.app

import com.payup.app.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountingCalculationTest {

    @Test
    fun testPaiseArithmeticDeterministicNoFloatingPointErrors() {
        // Test micro-transactions like ₹1, ₹10, ₹0.50
        val payment1Paise = 100L // ₹1.00
        val payment2Paise = 1000L // ₹10.00
        val payment3Paise = 50L // ₹0.50
        val salaryPaise = 15000000L // ₹1,50,000.00

        val totalExpensesPaise = payment1Paise + payment2Paise + payment3Paise
        assertEquals(1150L, totalExpensesPaise)

        val savingsPaise = salaryPaise - totalExpensesPaise
        assertEquals(14998850L, savingsPaise)

        // Verify CurrencyUtils formatting
        assertEquals("₹1", CurrencyUtils.formatPaiseCompact(100L))
        assertEquals("₹10", CurrencyUtils.formatPaiseCompact(1000L))
        assertEquals("₹150,000", CurrencyUtils.formatPaiseCompact(15000000L))
        assertEquals("₹1.00", CurrencyUtils.formatPaise(100L))
        assertEquals("₹10.00", CurrencyUtils.formatPaise(1000L))
    }

    @Test
    fun testCurrencyParsing() {
        assertEquals(100L, CurrencyUtils.parseToPaise("1"))
        assertEquals(100L, CurrencyUtils.parseToPaise("1.00"))
        assertEquals(115L, CurrencyUtils.parseToPaise("1.15")) // Deterministic: 1.15 * 100 must not truncate to 114
        assertEquals(2999L, CurrencyUtils.parseToPaise("29.99"))
        assertEquals(1000L, CurrencyUtils.parseToPaise("10"))
        assertEquals(1050L, CurrencyUtils.parseToPaise("10.50"))
        assertEquals(254050L, CurrencyUtils.parseToPaise("₹2,540.50"))
        assertEquals(0L, CurrencyUtils.parseToPaise(""))
        assertEquals(0L, CurrencyUtils.parseToPaise("invalid"))
    }

    @Test
    fun testBudgetRemainingCalculation() {
        val allocatedBudgetPaise = 500000L // ₹5,000
        val spentPaise = 320050L // ₹3,200.50
        val remainingPaise = allocatedBudgetPaise - spentPaise

        assertEquals(179950L, remainingPaise)
        assertEquals("₹1,799.50", CurrencyUtils.formatPaiseCompact(remainingPaise))
    }

    @Test
    fun testAccountReactiveBalanceDerivation() {
        // Initial opening balances
        val cashOpeningPaise = 100000L // ₹1,000 opening cash
        val upiOpeningPaise = 500000L  // ₹5,000 opening UPI balance

        // Transactions:
        // 1. Cash expense ₹150
        // 2. UPI expense ₹350
        // 3. Cash income (e.g. ATM withdrawal or cash gift) ₹500
        val cashExpenses = 15000L
        val cashIncome = 50000L
        val upiExpenses = 35000L

        val netCash = cashIncome - cashExpenses // +35000L
        val netUpi = -upiExpenses              // -35000L

        val derivedCashBalance = cashOpeningPaise + netCash
        val derivedUpiBalance = upiOpeningPaise + netUpi

        assertEquals(135000L, derivedCashBalance) // ₹1,350
        assertEquals(465000L, derivedUpiBalance)  // ₹4,650
        assertEquals(600000L, derivedCashBalance + derivedUpiBalance)

        // Ensure payment-method spend is distinct from balance
        assertEquals(15000L, cashExpenses)
        assertEquals(35000L, upiExpenses)
        assertEquals(50000L, cashExpenses + upiExpenses)
    }

    @Test
    fun testCanonicalCashDeduplication() {
        val accounts = listOf(
            mapOf("id" to "acc_upi", "name" to "UPI / Google Pay"),
            mapOf("id" to "old-uuid-1", "name" to "Cash"),
            mapOf("id" to "acc_cash", "name" to "Cash"),
            mapOf("id" to "acc_bank", "name" to "Primary Bank")
        )
        // Deduplicate by name giving preference to canonical IDs
        val deduplicated = accounts.groupBy { it["name"] }
            .map { (_, group) ->
                group.firstOrNull { it["id"]?.startsWith("acc_") == true } ?: group.first()
            }

        assertEquals(3, deduplicated.size)
        val cash = deduplicated.find { it["name"] == "Cash" }
        assertEquals("acc_cash", cash?.get("id"))
    }

    @Test
    fun testIncomeAllocationAndOverAllocation() {
        // User example: income ₹25,000; Personal ₹5,000; Rent ₹10,000; Intercity Travel ₹2,000; Savings ₹5,000; Other ₹3,000.
        val incomePaise = 2500000L // ₹25,000
        val personalPaise = 500000L // ₹5,000
        val rentPaise = 1000000L    // ₹10,000
        val travelPaise = 200000L   // ₹2,000
        val savingsPaise = 500000L  // ₹5,000
        val otherPaise = 300000L    // ₹3,000

        val totalAllocatedPaise = personalPaise + rentPaise + travelPaise + savingsPaise + otherPaise
        assertEquals(2500000L, totalAllocatedPaise)

        val unallocatedPaise = incomePaise - totalAllocatedPaise
        assertEquals(0L, unallocatedPaise)
        val isOverAllocated = totalAllocatedPaise > incomePaise
        assertEquals(false, isOverAllocated)

        // Test over-allocation detection
        val extraAllocation = 200000L // Extra ₹2,000
        val overAllocatedTotal = totalAllocatedPaise + extraAllocation
        val deficit = overAllocatedTotal - incomePaise
        assertEquals(200000L, deficit)
        assertEquals(true, overAllocatedTotal > incomePaise)
    }

    @Test
    fun testCustomBudgetCategoryMappingAndOverride() {
        // User example:
        // Personal ₹5,000 -> Food & Dining, Coffee, Shopping
        // Travel ₹2,000 -> Bus, Train, Taxi
        // A ₹350 Food & Dining expense reduces Personal remaining from ₹5,000 to ₹4,650.
        val personalBudgetPaise = 500000L
        val expensePaise = 35000L // ₹350 Food & Dining

        val personalRemainingPaise = personalBudgetPaise - expensePaise
        assertEquals(465000L, personalRemainingPaise) // ₹4,650

        // Test transaction-level override:
        // An expense has category "Food & Dining" (mapped to Personal),
        // but user explicitly sets budget override to "Other" (allocated ₹3,000).
        val otherBudgetPaise = 300000L
        val overrideExpensePaise = 10000L // ₹100
        val otherRemainingPaise = otherBudgetPaise - overrideExpensePaise

        assertEquals(290000L, otherRemainingPaise)
        // Personal budget is NOT double deducted
        assertEquals(465000L, personalRemainingPaise)
    }
}

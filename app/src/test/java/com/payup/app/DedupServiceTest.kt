package com.payup.app

import com.payup.app.service.DedupService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DedupServiceTest {

    @Test
    fun testSameTransactionGeneratesIdenticalHash() {
        val amount = 100L // ₹1
        val merchant = "Chai Tapri"
        val time = 1761800000000L
        val ref = "UPI123456789012"

        val hash1 = DedupService.generateHash(amount, merchant, time, ref)
        val hash2 = DedupService.generateHash(amount, merchant, time + 20000, ref) // 20s later in same minute

        assertEquals(hash1, hash2)
    }

    @Test
    fun testDifferentAmountsGenerateDifferentHash() {
        val merchant = "Swiggy"
        val time = 1761800000000L

        val hash1 = DedupService.generateHash(100L, merchant, time, null)
        val hash2 = DedupService.generateHash(1000L, merchant, time, null)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testMerchantCaseInsensitivity() {
        val amount = 50000L
        val time = 1761800000000L

        val hash1 = DedupService.generateHash(amount, "Google Pay", time, null)
        val hash2 = DedupService.generateHash(amount, "google pay  ", time, null)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testSameReferenceIdGeneratesIdenticalHashAcrossDifferentMerchants() {
        val hashNotification = DedupService.generateHash(15000L, "Swiggy", 1761800000000L, "432198765432")
        val hashSms = DedupService.generateHash(15000L, "Bank Payment", 1761800060000L, "432198765432")

        assertEquals(hashNotification, hashSms)
    }
}

package com.payup.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.DetectionMethod
import com.payup.app.data.model.Transaction
import com.payup.app.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PayUpSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBody = messages.joinToString("") { it.messageBody ?: "" }
        val sender = messages[0].originatingAddress ?: ""

        val parsed = SmsParser.parse(sender, fullBody) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PayUpDatabase.getInstance(context.applicationContext)
                val transactionRepo = TransactionRepository(db.transactionDao())
                val merchantCategorizer = MerchantCategorizer(db.merchantRuleDao())
                val pendingObservationDao = db.pendingObservationDao()

                // Conservative matching by reference ID
                if (parsed.referenceId != null) {
                    val existing = transactionRepo.findByReferenceId(parsed.referenceId)
                    if (existing != null) {
                        // Already recorded from UPI notification! Enrich rather than duplicate
                        if (existing.accountId == null || existing.accountId == "acc_upi") {
                            transactionRepo.update(existing.copy(notes = (existing.notes?.let { "$it | " } ?: "") + "Verified by ${parsed.bankName} SMS"))
                        }
                        return@launch
                    }

                    // Check if an incomplete pending observation can be promoted with verified amount
                    val obs = pendingObservationDao.findPendingByRef(parsed.referenceId)
                    if (obs != null) {
                        pendingObservationDao.update(obs.copy(status = com.payup.app.data.model.ObservationStatus.RESOLVED))
                    }
                }

                val dedupHash = DedupService.generateHash(
                    parsed.amountPaise,
                    parsed.merchant,
                    parsed.timestamp.toEpochMilli(),
                    parsed.referenceId
                )

                // Double check dedup hash
                val hashMatch = transactionRepo.findByDedupHash(dedupHash)
                if (hashMatch != null) return@launch

                val categoryId = merchantCategorizer.categorize(parsed.merchant)

                val transaction = Transaction(
                    amountPaise = parsed.amountPaise,
                    type = parsed.type,
                    status = parsed.status,
                    merchant = parsed.merchant,
                    categoryId = categoryId,
                    accountId = "acc_bank",
                    timestamp = parsed.timestamp,
                    sourceApp = "${parsed.bankName} (SMS)",
                    referenceId = parsed.referenceId,
                    detectionMethod = DetectionMethod.SMS,
                    dedupHash = dedupHash
                )

                transactionRepo.insertIfNotDuplicate(transaction)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

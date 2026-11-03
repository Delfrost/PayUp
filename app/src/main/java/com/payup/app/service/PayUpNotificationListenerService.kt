package com.payup.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.*
import com.payup.app.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PayUpNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var merchantCategorizer: MerchantCategorizer
    private lateinit var pendingObservationDao: com.payup.app.data.db.PendingObservationDao

    private val monitoredPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "money.super.payments",                     // super.money
        "net.one97.paytm",                          // Paytm
        "com.phonepe.app",                          // PhonePe
        "in.org.npci.upiapp",                       // BHIM
        "com.whatsapp",                             // WhatsApp Pay
        "in.amazon.mShop.android.shopping"          // Amazon Pay
    )

    override fun onCreate() {
        super.onCreate()
        val db = PayUpDatabase.getInstance(applicationContext)
        transactionRepository = TransactionRepository(db.transactionDao())
        merchantCategorizer = MerchantCategorizer(db.merchantRuleDao())
        pendingObservationDao = db.pendingObservationDao()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in monitoredPackages) return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()

        val result = NotificationParser.parseWithObservation(sbn.packageName, title, text) ?: return

        serviceScope.launch {
            when (result) {
                is NotificationParser.ParseResult.Confirmed -> {
                    val parsed = result.transaction

                    // Check if a Bank SMS already recorded this transaction by reference ID
                    if (parsed.referenceId != null) {
                        val existing = transactionRepository.findByReferenceId(parsed.referenceId)
                        if (existing != null) {
                            // Already recorded from Bank SMS or prior alert. Enrich rather than duplicate
                            transactionRepository.update(
                                existing.copy(
                                    merchant = if (existing.merchant == "Bank Payment" || existing.merchant.isBlank()) parsed.merchant else existing.merchant,
                                    sourceApp = "${existing.sourceApp} & ${parsed.sourceApp}"
                                )
                            )
                            return@launch
                        }
                    }

                    val dedupHash = DedupService.generateHash(
                        parsed.amountPaise,
                        parsed.merchant,
                        parsed.timestamp.toEpochMilli(),
                        parsed.referenceId
                    )

                    val categoryId = merchantCategorizer.categorize(parsed.merchant)

                    val transaction = Transaction(
                        amountPaise = parsed.amountPaise,
                        type = parsed.type,
                        status = parsed.status,
                        merchant = parsed.merchant,
                        categoryId = categoryId,
                        accountId = "acc_upi",
                        timestamp = parsed.timestamp,
                        sourceApp = parsed.sourceApp,
                        referenceId = parsed.referenceId,
                        detectionMethod = DetectionMethod.NOTIFICATION,
                        dedupHash = dedupHash
                    )

                    transactionRepository.insertIfNotDuplicate(transaction)

                    // If reference ID matched a previous incomplete observation, resolve it
                    parsed.referenceId?.let { ref ->
                        val obs = pendingObservationDao.findPendingByRef(ref)
                        if (obs != null) {
                            pendingObservationDao.update(obs.copy(status = ObservationStatus.RESOLVED))
                        }
                    }
                }

                is NotificationParser.ParseResult.Incomplete -> {
                    // Non-posted pending observation (never fabricate ₹0 expense)
                    pendingObservationDao.insert(result.observation)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed
    }
}

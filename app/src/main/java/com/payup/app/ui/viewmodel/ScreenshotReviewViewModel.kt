package com.payup.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.payup.app.data.db.PayUpDatabase
import com.payup.app.data.model.*
import com.payup.app.repository.AccountRepository
import com.payup.app.repository.CategoryRepository
import com.payup.app.repository.TransactionRepository
import com.google.android.gms.tasks.Tasks
import com.payup.app.service.DedupService
import com.payup.app.service.MerchantCategorizer
import com.payup.app.service.ScreenshotCandidate
import com.payup.app.service.ScreenshotParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScreenshotReviewState(
    val isScanning: Boolean = false,
    val candidates: List<ScreenshotCandidate> = emptyList(),
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false,
    val error: String? = null
)

class ScreenshotReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PayUpDatabase.getInstance(application)
    private val transactionRepo = TransactionRepository(db.transactionDao())
    private val categoryRepo = CategoryRepository(db.categoryDao())
    private val accountRepo = AccountRepository(db.accountDao())
    private val merchantCategorizer = MerchantCategorizer(db.merchantRuleDao())

    private val _state = MutableStateFlow(ScreenshotReviewState())
    val state: StateFlow<ScreenshotReviewState> = _state.asStateFlow()

    val categories: Flow<List<Category>> = categoryRepo.getAll()
    val accounts: Flow<List<Account>> = accountRepo.getAll()

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun processScreenshots(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, error = null)
            val extractedList = mutableListOf<ScreenshotCandidate>()

            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    try {
                        val image = InputImage.fromFilePath(getApplication(), uri)
                        val result = Tasks.await(textRecognizer.process(image))
                        val candidates = ScreenshotParser.parseText(result.text)

                        for (cand in candidates) {
                            // Auto categorize merchant
                            val catId = merchantCategorizer.categorize(cand.merchant)
                            cand.categoryId = catId

                            // Check for duplicates by reference ID or hash
                            if (cand.referenceId != null) {
                                val existingByRef = transactionRepo.findByReferenceId(cand.referenceId!!)
                                if (existingByRef != null) {
                                    cand.isLikelyDuplicate = true
                                }
                            }

                            val hash = DedupService.generateHash(
                                cand.amountPaise,
                                cand.merchant,
                                cand.timestamp.toEpochMilli(),
                                cand.referenceId
                            )
                            val existing = transactionRepo.findByDedupHash(hash)
                            if (existing != null) {
                                cand.isLikelyDuplicate = true
                            }
                            extractedList.add(cand)
                        }
                    } catch (e: Exception) {
                        // Continue parsing other images if one fails
                    }
                }
            }

            _state.value = _state.value.copy(
                isScanning = false,
                candidates = extractedList,
                error = if (extractedList.isEmpty()) "No transactions found in selected screenshot(s)." else null
            )
        }
    }

    fun updateCandidate(id: String, transform: (ScreenshotCandidate) -> ScreenshotCandidate) {
        val updated = _state.value.candidates.map {
            if (it.id == id) transform(it) else it
        }
        _state.value = _state.value.copy(candidates = updated)
    }

    fun toggleExclude(id: String) {
        updateCandidate(id) { it.copy(isExcluded = !it.isExcluded) }
    }

    fun deleteCandidate(id: String) {
        val updated = _state.value.candidates.filter { it.id != id }
        _state.value = _state.value.copy(candidates = updated)
    }

    fun selectAll() {
        val updated = _state.value.candidates.map { it.copy(isExcluded = false) }
        _state.value = _state.value.copy(candidates = updated)
    }

    fun deselectAll() {
        val updated = _state.value.candidates.map { it.copy(isExcluded = true) }
        _state.value = _state.value.copy(candidates = updated)
    }

    fun confirmImport() {
        val toImport = _state.value.candidates.filter { !it.isExcluded }
        if (toImport.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isImporting = true)
            try {
                for (cand in toImport) {
                    val hash = DedupService.generateHash(
                        cand.amountPaise,
                        cand.merchant,
                        cand.timestamp.toEpochMilli(),
                        cand.referenceId
                    )
                    val transaction = Transaction(
                        amountPaise = cand.amountPaise,
                        type = cand.type,
                        status = cand.status,
                        merchant = cand.merchant,
                        categoryId = cand.categoryId,
                        accountId = cand.accountId ?: "acc_upi",
                        timestamp = cand.timestamp,
                        referenceId = cand.referenceId,
                        sourceApp = cand.sourceApp,
                        detectionMethod = DetectionMethod.SCREENSHOT_OCR,
                        dedupHash = hash
                    )
                    transactionRepo.insert(transaction)
                }
                _state.value = _state.value.copy(isImporting = false, importSuccess = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isImporting = false, error = e.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}

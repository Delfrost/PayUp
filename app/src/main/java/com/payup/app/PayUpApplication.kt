package com.payup.app

import android.app.Application
import com.payup.app.data.db.PayUpDatabase

class PayUpApplication : Application() {
    val database: PayUpDatabase by lazy { PayUpDatabase.getInstance(this) }
}

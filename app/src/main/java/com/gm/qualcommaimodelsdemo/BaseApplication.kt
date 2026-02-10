package com.gm.qualcommaimodelsdemo

import android.app.Application
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.info
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication: Application(), Logs {
    override var TAG: String = "Base Application"

    override fun onCreate() {
        super.onCreate()
        info("onCreate")
    }

    override fun onTerminate() {
        super.onTerminate()
        info("Terminated")
    }
}
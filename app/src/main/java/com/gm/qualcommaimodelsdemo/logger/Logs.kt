package com.gm.qualcommaimodelsdemo.logger

import android.util.Log
import com.gm.qualcommaimodelsdemo.BuildConfig

interface Logs {
    var TAG: String
}

fun Logs.debug(msg: String) {
    if (BuildConfig.DEBUG)
        Log.d(TAG, msg)
}

fun Logs.error(msg: String) {
    Log.e(TAG, msg)
}

fun Logs.warning(msg: String) {
    Log.w(TAG, msg)
}

fun Logs.info(msg: String) {
    Log.i(TAG, msg)
}
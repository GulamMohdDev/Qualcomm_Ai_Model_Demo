package com.gm.qualcommaimodelsdemo.entity

import android.graphics.RectF

data class DetectionResult(
    val boundingBox: RectF,
    val label: String,
    val score: Float
)

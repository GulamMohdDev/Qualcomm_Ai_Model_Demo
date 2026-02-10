package com.gm.qualcommaimodelsdemo.repository.repo_interface

import android.graphics.Bitmap

interface SemanticSegmentationAiModelRepository {
    suspend fun segment(bitmap: Bitmap): Bitmap
    fun close()
}
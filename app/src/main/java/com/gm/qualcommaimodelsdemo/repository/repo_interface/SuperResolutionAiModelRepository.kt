package com.gm.qualcommaimodelsdemo.repository.repo_interface

import android.graphics.Bitmap

interface SuperResolutionAiModelRepository {
    suspend fun upscale(bitmap: Bitmap): Bitmap
    fun close()
}
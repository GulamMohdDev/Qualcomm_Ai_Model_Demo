package com.gm.qualcommaimodelsdemo.repository.repo_interface

import android.graphics.Bitmap

interface ObjectDetectionAiModelRepository {

    suspend fun detectObject(bitmap: Bitmap) : Bitmap
    suspend fun classifyObject(bitmap: Bitmap) : Bitmap

    fun close()
}
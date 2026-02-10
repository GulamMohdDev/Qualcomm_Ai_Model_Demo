package com.gm.qualcommaimodelsdemo.repository.repo_interface

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

interface DepthAiModelRepository {
    fun initialize()
    fun predict(bitmap: Bitmap, callback: (Bitmap) -> Unit)
    fun close()
}
package com.gm.qualcommaimodelsdemo.repository.repo_imp

import android.content.Context
import android.graphics.Bitmap
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.info
import com.gm.qualcommaimodelsdemo.repository.repo_interface.DepthAiModelRepository
import com.gm.qualcommaimodelsdemo.utils.ai_utils.DepthAiModelUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DepthAiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DepthAiModelRepository, Logs {
    override var TAG: String = "DepthAiModelRepositoryImpl"
    private var depthAiModelUtils: DepthAiModelUtils = DepthAiModelUtils(context)

    init {
        info("DepthAiModelRepositoryImpl initialized")
    }

    override fun initialize() {
        depthAiModelUtils.initialize()
    }

    override fun predict(
        bitmap: Bitmap,
        callback: (Bitmap) -> Unit
    ) {
        depthAiModelUtils.predict(bitmap, callback)
    }

    override fun close() {
        depthAiModelUtils.close()
    }

}
package com.gm.qualcommaimodelsdemo.repository.repo_imp

import android.content.Context
import android.graphics.Bitmap
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.repository.repo_interface.SuperResolutionAiModelRepository
import com.gm.qualcommaimodelsdemo.utils.ai_utils.SuperResolutionAiModelUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SuperResolutionAiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SuperResolutionAiModelRepository, Cloneable, Logs {
    override var TAG: String = "SuperResolutionAiModelRepositoryImpl"

    val superResolutionAiModelUtils = SuperResolutionAiModelUtils(context)

    override suspend fun upscale(bitmap: Bitmap): Bitmap {
        return superResolutionAiModelUtils.upscale(bitmap)
    }

    override fun close() {
        superResolutionAiModelUtils.close()
    }
}
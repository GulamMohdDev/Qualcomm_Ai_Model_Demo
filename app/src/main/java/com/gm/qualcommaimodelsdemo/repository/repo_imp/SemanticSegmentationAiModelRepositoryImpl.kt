package com.gm.qualcommaimodelsdemo.repository.repo_imp

import android.content.Context
import android.graphics.Bitmap
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.repository.repo_interface.SemanticSegmentationAiModelRepository
import com.gm.qualcommaimodelsdemo.utils.ai_utils.SemanticSegmentationAiModelUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SemanticSegmentationAiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
): SemanticSegmentationAiModelRepository, Cloneable, Logs {
    override var TAG: String ="SemanticSegmentationAiModelRepositoryImpl"
    val semanticSegmentationAiModelUtils = SemanticSegmentationAiModelUtils(context)
    override suspend fun segment(bitmap: Bitmap): Bitmap {
        return semanticSegmentationAiModelUtils.segment(bitmap)
    }

    override fun close() {
        semanticSegmentationAiModelUtils.close()
    }


}
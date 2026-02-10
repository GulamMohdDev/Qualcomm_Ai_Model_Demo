package com.gm.qualcommaimodelsdemo.repository.repo_imp

import android.content.Context
import android.graphics.Bitmap
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.repository.repo_interface.ObjectDetectionAiModelRepository
import com.gm.qualcommaimodelsdemo.utils.ai_utils.ImageClassificationUtils
import com.gm.qualcommaimodelsdemo.utils.ai_utils.ObjectDetectionAiModelUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ObjectDetectionAiModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) :
    ObjectDetectionAiModelRepository, Logs {

    override var TAG: String = "ObjectDetectionAiModelRepositoryImpl"
    val objectDetectionAiModelUtils = ObjectDetectionAiModelUtils(context)
    val imageClassificationUtils = ImageClassificationUtils(context)

    override suspend fun detectObject(bitmap: Bitmap) : Bitmap {
        return objectDetectionAiModelUtils.detect(bitmap)
    }

    override suspend fun classifyObject(bitmap: Bitmap): Bitmap {
        return imageClassificationUtils.classify(bitmap)
    }

    override fun close() {
        objectDetectionAiModelUtils.close()
    }
}
package com.gm.qualcommaimodelsdemo.repository.repo_imp

import android.content.Context
import android.net.Uri
import com.gm.qualcommaimodelsdemo.entity.VideoAction
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.repository.repo_interface.VideoClassificationModelRepository
import com.gm.qualcommaimodelsdemo.utils.ai_utils.VideoClassificationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class VideoClassificationModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoClassificationModelRepository, Logs {
    override var TAG: String = "VideoClassificationModelRepositoryImpl"
    val videoClassificationUtils by lazy { VideoClassificationUtils(context) }
    override suspend fun classifyVideo(videoUri: Uri): List<VideoAction> {
        return videoClassificationUtils.classifyVideo(videoUri)
    }

    override fun close() {
        videoClassificationUtils.close()
    }
}
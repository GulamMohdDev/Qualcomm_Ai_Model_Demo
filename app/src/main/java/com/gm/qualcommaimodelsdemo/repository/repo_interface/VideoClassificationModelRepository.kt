package com.gm.qualcommaimodelsdemo.repository.repo_interface

import android.net.Uri
import com.gm.qualcommaimodelsdemo.entity.VideoAction

interface VideoClassificationModelRepository {
    suspend fun classifyVideo(videoUri: Uri): List<VideoAction>
    fun close()
}
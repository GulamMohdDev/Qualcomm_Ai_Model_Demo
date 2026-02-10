package com.gm.qualcommaimodelsdemo.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.repository.repo_interface.VideoClassificationModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoClassificationScreenViewModel @Inject constructor(
    private val videoClassificationModelRepository: VideoClassificationModelRepository
) : ViewModel(), Logs {
    override var TAG = "VideoClassificationScreenViewModel"

    private val _estimatedOutputTime = MutableStateFlow<String?>(null)
    val estimatedOutputTime = _estimatedOutputTime.asStateFlow()

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri = _videoUri.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result = _result.asStateFlow()


    fun setVideoUri(uri: Uri?) {
        _videoUri.value = uri
    }

    fun classifyVideo() {
        _estimatedOutputTime.value = "Processing Video..."
        val startTime = System.currentTimeMillis()
        videoUri.value?.let {
            viewModelScope.launch {
                var outputString = ""
                val output = videoClassificationModelRepository.classifyVideo(it)
                for (videoAction in output) {
                    outputString += "${videoAction.action}  ${videoAction.score} \n"
                }
                _result.value = outputString
                val endTime = System.currentTimeMillis()
                val time = endTime - startTime
                _estimatedOutputTime.value = "Estimated Output Time: $time ms"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoClassificationModelRepository.close()
    }
}

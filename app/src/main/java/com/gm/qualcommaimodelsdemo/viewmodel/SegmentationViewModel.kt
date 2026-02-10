package com.gm.qualcommaimodelsdemo.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.repository.repo_interface.SemanticSegmentationAiModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SegmentationViewModel @Inject constructor(
    private val semanticSegmentationAiModelRepository: SemanticSegmentationAiModelRepository
) : ViewModel(), Logs {
    override var TAG: String = "SegmentationViewModel"
    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap = _imageBitmap.asStateFlow()

    private val _outputImageBitmap = MutableStateFlow<Bitmap?>(null)
    val outputImageBitmap = _outputImageBitmap.asStateFlow()

    private val _estimatedOutputTime = MutableStateFlow<String?>(null)
    val estimatedOutputTime = _estimatedOutputTime.asStateFlow()

    // Function to handle the URI and convert to Bitmap/ImageBitmap
    fun onImagePicked(context: Context, uri: Uri?) {
        if (uri == null) return

        // Use a coroutine for the blocking I/O operation
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert Uri to Bitmap
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                // Update the state on the main thread
                withContext(Dispatchers.Main) {
                    _imageBitmap.value = bitmap
                }
            } catch (e: Exception) {
                // Handle error (e.g., log it or update an error state)
                e.printStackTrace()
            }
        }
    }

    fun onImageCaptured(bitmap: Bitmap) {
        _imageBitmap.value = bitmap
    }

    fun clearImage() {
        _imageBitmap.value = null
    }

    fun checkSemanticSegmentation(bitmap: Bitmap) {
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            val output = semanticSegmentationAiModelRepository.segment(bitmap)
            _outputImageBitmap.value = output
            val endTime = System.currentTimeMillis()
            val time = endTime - startTime
            _estimatedOutputTime.value = "Estimated Output Time: $time ms"
        }
    }

    override fun onCleared() {
        super.onCleared()
        semanticSegmentationAiModelRepository.close()
    }

}
package com.gm.qualcommaimodelsdemo.utils.ai_utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.Closeable


class DepthAiModelUtils(private val context: Context) : Logs, Closeable {
    override var TAG: String = "DepthAiModelUtils"

    private lateinit var interpreter: Interpreter
    private lateinit var inputImage: TensorImage
    private lateinit var outputBuffer: TensorBuffer

    private var modelInputWidth = 0
    private var modelInputHeight = 0

    fun initialize() {
        try {
            val model = FileUtil.loadMappedFile(context, "midas-depth.tflite")
            val compatList = CompatibilityList()
            val options = Interpreter.Options().apply {
                setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
                useNNAPI = true
            }
            if (compatList.isDelegateSupportedOnThisDevice) {
                options.addDelegateFactory(GpuDelegateFactory()) //gpu Supported Devices
            } else {
                options.setNumThreads(4)
            }
            // 2. Try to Initialize with GPU
            try {
                // Create the GPU Delegate
                interpreter = Interpreter(model, options)
                debug("Successfully initialized model with GPU Delegate")
            } catch (e: Exception) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                    useNNAPI = true
                }
                interpreter = Interpreter(model, options)
                debug("Initialized model with CPU")
            }

            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            // Assuming input tensor format is [1, height, width, 3]
            modelInputHeight = inputShape[1]
            modelInputWidth = inputShape[2]

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()

            inputImage = TensorImage(inputTensor.dataType())
            outputBuffer = TensorBuffer.createFixedSize(outputShape, outputTensor.dataType())

        } catch (e: Exception) {
            debug("Fatal error initializing model: ${e.message}")
        }
    }

    fun predict(bitmap: Bitmap, callback: (Bitmap) -> Unit) {
        if (!::interpreter.isInitialized) {
            debug("Interpreter not initialized, skipping prediction")
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val imageProcessor = ImageProcessor.Builder()
                    .add(
                        ResizeOp(
                            modelInputHeight,
                            modelInputWidth,
                            ResizeOp.ResizeMethod.BILINEAR
                        )
                    )
                    .add(NormalizeOp(0f, 255f))
                    .build()

                inputImage.load(bitmap)
                val processedImage = imageProcessor.process(inputImage)

                interpreter.run(processedImage.buffer, outputBuffer.buffer.rewind())

                val depthBitmap = convertOutputToBitmap(outputBuffer)

                CoroutineScope(Dispatchers.Main).launch {
                    callback(depthBitmap)
                }
            } catch (e: Exception) {
                debug("Error during prediction: ${e.message}")
            }
        }
    }

    private fun convertOutputToBitmap(output: TensorBuffer): Bitmap {
        val outputArray = output.floatArray
        // Assuming output tensor format is [1, height, width, 1]
        val height = output.shape[1]
        val width = output.shape[2]

        // Find min and max values for normalization
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        for (value in outputArray) {
            if (value < min) min = value
            if (value > max) max = value
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Avoid division by zero if output is flat
        val range = if (max - min == 0f) 1f else max - min

        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = outputArray[y * width + x]
                // Normalize the depth value to a 0-255 range
                val gray = ((value - min) / range * 255.0f).toInt()
                bitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return bitmap
    }

    override fun close() {
        if (::interpreter.isInitialized) {
            interpreter.close()
        }
    }
}
package com.gm.qualcommaimodelsdemo.utils.ai_utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder


class SuperResolutionAiModelUtils(private val context: Context) : Logs, Closeable {

    override var TAG = "SuperResUtils"

    companion object {
        private const val MODEL_FILE = "quicksrnetlarge-quicksrnetlarge-float.tflite"
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    private var inputWidth = 0
    private var inputHeight = 0
    private var outputWidth = 0
    private var outputHeight = 0

    init {
        initModel()
    }

    private fun initModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)

            // 1. Initialize with GPU
            try {
                gpuDelegate = GpuDelegate()
                val options = Interpreter.Options()
                options.addDelegate(gpuDelegate)
                interpreter = Interpreter(modelFile, options)
                Log.d(TAG, "Initialized with GPU")
            } catch (e: Exception) {
                gpuDelegate?.close()
                gpuDelegate = null
                val options = Interpreter.Options()
                options.setNumThreads(4)
                interpreter = Interpreter(modelFile, options)
                error("Initialized with CPU (GPU failed)\n${e.message}")
            }

            // 2. Read Input Shape
            val inputTensor = interpreter?.getInputTensor(0)
            val inShape = inputTensor?.shape()
            if (inShape != null && inShape.size == 4) {
                inputHeight = inShape[1]
                inputWidth = inShape[2]
            }

            // 3. Read Output Shape
            val outputTensor = interpreter?.getOutputTensor(0)
            val outShape = outputTensor?.shape()
            if (outShape != null && outShape.size == 4) {
                outputHeight = outShape[1]
                outputWidth = outShape[2]
            }

            debug("Model Input: ${inputWidth}x${inputHeight} -> Output: ${outputWidth}x${outputHeight}")

        } catch (e: Exception) {
            error("Error initializing: ${e.message}")
        }
    }

    suspend fun upscale(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (interpreter == null || inputWidth == 0) return@withContext bitmap

        try {
            val inputBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }

            // --- 1. Calculate Proportional Resize (To avoid stretching) ---
            val ratio = inputBitmap.width.toFloat() / inputBitmap.height.toFloat()
            var targetHeight = inputHeight
            var targetWidth = inputWidth

            if (inputBitmap.width > inputBitmap.height) {
                // Landscape: Fit Width, shrink Height
                targetHeight = (inputWidth / ratio).toInt()
            } else {
                // Portrait: Fit Height, shrink Width
                targetWidth = (inputHeight * ratio).toInt()
            }

            // --- 2. Pre-process ---
            val imageProcessor = ImageProcessor.Builder()
                // A. Resize to fit inside the square WITHOUT changing aspect ratio
                .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
                // B. Pad with Black Bars to fill the rest of the 128x128 box
                .add(ResizeWithCropOrPadOp(inputHeight, inputWidth))
                // C. Normalize
                .add(NormalizeOp(0f, 255f))
                .build()

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(inputBitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // --- 3. Run Inference ---
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes())
            outputBuffer.order(ByteOrder.nativeOrder())

            interpreter?.run(tensorImage.buffer, outputBuffer)

            // --- 4. Post-process ---
            outputBuffer.rewind()
            val pixels = IntArray(outputWidth * outputHeight)

            for (i in 0 until (outputWidth * outputHeight)) {
                val r = (outputBuffer.float * 255f).toInt().coerceIn(0, 255)
                val g = (outputBuffer.float * 255f).toInt().coerceIn(0, 255)
                val b = (outputBuffer.float * 255f).toInt().coerceIn(0, 255)
                pixels[i] = Color.rgb(r, g, b)
            }

            // Final Result (512x512 with Black Bars)
            // You can crop the black bars here if you really want, but this returns the full upscale context
            return@withContext Bitmap.createBitmap(pixels, outputWidth, outputHeight, Bitmap.Config.ARGB_8888)

        } catch (e: Exception) {
            error("Error upscaling: ${e.message}")
            return@withContext bitmap
        }
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
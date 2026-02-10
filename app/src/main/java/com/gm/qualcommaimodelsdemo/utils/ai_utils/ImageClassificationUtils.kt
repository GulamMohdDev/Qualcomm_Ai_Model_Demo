package com.gm.qualcommaimodelsdemo.utils.ai_utils

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.gm.qualcommaimodelsdemo.entity.ClassificationResult
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue

// Simple data class for Classification results

class ImageClassificationUtils(private val context: Context) : Logs, Closeable {
    override var TAG: String = "ImageClassificationUtils"

    companion object {
        // ✅ Make sure this matches your Qualcomm Classification model name
        private const val MODEL_FILE = "mobilenet_v2.tflite"
        private const val LABELS_FILE = "labels.txt" // Standard ImageNet 1000 labels
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var labels: List<String> = emptyList()

    // Standard input size for Classification (usually 224 or 229)
    private var inputImageWidth: Int = 224
    private var inputImageHeight: Int = 224

    init {
        initModel()
    }

    private fun initModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)

            // 1. Try Initialize GPU
            try {
                gpuDelegate = GpuDelegate()
                val options = Interpreter.Options()
                options.addDelegate(gpuDelegate)
                interpreter = Interpreter(modelFile, options)
                debug("Initialized with GPU Delegate")
            } catch (e: Exception) {
                // Fallback to CPU
                gpuDelegate?.close()
                gpuDelegate = null
                val options = Interpreter.Options()
                options.setNumThreads(4)
                interpreter = Interpreter(modelFile, options)
                debug("Initialized with CPU")
            }

            // 2. Read Input Shape automatically
            val inputTensor = interpreter?.getInputTensor(0)
            val shape = inputTensor?.shape()
            if (shape != null && shape.size == 4) {
                inputImageHeight = shape[1] // Usually 224
                inputImageWidth = shape[2]  // Usually 224
            }

            // 3. Load Labels (Must have 1000 lines for ImageNet models)
            labels = FileUtil.loadLabels(context, LABELS_FILE)

        } catch (e: Exception) {
            debug("Fatal Error initializing model: ${e.message}")
        }
    }

    suspend fun classify(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (interpreter == null) return@withContext bitmap

        try {
            // --- 1. Pre-process Input ---
            val inputTensor = interpreter!!.getInputTensor(0)

            val imageProcessorBuilder = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))

            // IMPORTANT: Normalization depends on the specific Qualcomm model.
            // Most Float32 models need values between 0.0-1.0 or -1.0-1.0
            if (inputTensor.dataType() == DataType.FLOAT32) {
                // Try Option A first (0 to 1). If accuracy is bad, try Option B.

                // Option A: [0, 1]
                imageProcessorBuilder.add(NormalizeOp(0f, 255f))

                // Option B: [-1, 1] (Common for MobileNet V1/V2)
                // imageProcessorBuilder.add(NormalizeOp(127.5f, 127.5f))
            }

            val imageProcessor = imageProcessorBuilder.build()
            var tensorImage = TensorImage(inputTensor.dataType())
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // --- 2. Prepare Output Buffer ---
            // Output shape is [1, 1000]
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes())
            outputBuffer.order(ByteOrder.nativeOrder())

            // --- 3. Run Inference ---
            interpreter?.run(tensorImage.buffer, outputBuffer)

            // --- 4. Post-process (Find Top 1 or Top 3) ---
            outputBuffer.rewind()
            val results = getTopKLabels(outputBuffer, outputTensor.dataType(), 3) // Get Top 3

            // --- 5. Draw Text on Bitmap ---
            return@withContext drawResults(bitmap, results)

        } catch (e: Exception) {
            debug("Error during classification: ${e.message}")
            return@withContext bitmap
        }
    }

    /**
     * Helper to find the highest probabilities in the output array
     */
    private fun getTopKLabels(
        buffer: ByteBuffer,
        dataType: DataType,
        k: Int
    ): List<ClassificationResult> {
        // Priority Queue to keep the top K results
        val pq = PriorityQueue<ClassificationResult>(k) { o1, o2 -> (o1.score).compareTo(o2.score) }

        val isFloat = (dataType == DataType.FLOAT32)
        val isQuantized = (dataType == DataType.UINT8)

        // Get Quantization params just in case
        val params = interpreter!!.getOutputTensor(0).quantizationParams()

        for (i in labels.indices) {
            var score = 0f
            if (isFloat) {
                score = buffer.getFloat(i * 4)
            } else if (isQuantized) {
                val byteVal = buffer[i].toInt() and 0xFF
                if (params.scale > 0f) {
                    score = (byteVal - params.zeroPoint) * params.scale
                } else {
                    score = byteVal.toFloat() // Fallback
                }
            }

            // Optimization: Only add if score is meaningful
            if (score > 0.1f) {
                val label = if (i < labels.size) labels[i] else "Class $i"
                pq.add(ClassificationResult(label, score))
                if (pq.size > k) {
                    pq.poll() // Remove lowest score
                }
            }
        }

        // Convert to list and reverse (Highest first)
        val resultList = ArrayList<ClassificationResult>()
        while (pq.isNotEmpty()) {
            resultList.add(pq.poll())
        }
        return resultList.reversed()
    }

    private fun drawResults(bitmap: Bitmap, results: List<ClassificationResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isAntiAlias = true
            style = Paint.Style.FILL
            setShadowLayer(5f, 0f, 0f, Color.BLACK) // Black shadow for visibility
        }

        var yPos = 100f
        for (result in results) {
            val text = "${result.label}: ${(result.score * 10).toInt()}%"
            canvas.drawText(text, 50f, yPos, textPaint)
            yPos += 20f
        }

        if (results.isEmpty()) {
            canvas.drawText("No confident result", 50f, 100f, textPaint)
        }

        return mutableBitmap
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
package com.gm.qualcommaimodelsdemo.utils.ai_utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.gm.qualcommaimodelsdemo.entity.DetectionResult
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectDetectionAiModelUtils(private val context: Context) : Logs, Closeable {
    override var TAG: String = "ObjectDetectionAiModelUtils"

    companion object {
        private const val MODEL_FILE = "object_detection.tflite" // Must be SSD MobileNet
        private const val LABELS_FILE = "labelmapobjects.txt"
        private const val MIN_CONFIDENCE = 0.5f // 50% threshold
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var labels: List<String> = emptyList()

    // Default size, will be overwritten by model
    private var inputImageWidth: Int = 300
    private var inputImageHeight: Int = 300

    init {
        initModel()
    }

    private fun initModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)

            // 1. Initialize GPU Delegate (Crucial for Qualcomm)
            try {
                gpuDelegate = GpuDelegate()
                val options = Interpreter.Options()
                options.addDelegate(gpuDelegate)
                interpreter = Interpreter(modelFile, options)
                debug("Initialized with GPU Delegate")
            } catch (e: Exception) {
                // Fallback to CPU if GPU fails
                gpuDelegate?.close()
                gpuDelegate = null
                val options = Interpreter.Options()
                interpreter = Interpreter(modelFile, options)
                debug("Initialized with CPU (GPU failed)")
            }

            // 2. Read Input Shape
            val inputTensor = interpreter?.getInputTensor(0)
            val shape = inputTensor?.shape()
            if (shape != null && shape.size == 4) {
                inputImageHeight = shape[1] // Usually 300
                inputImageWidth = shape[2]  // Usually 300
            }

            // 3. Load Labels
            labels = FileUtil.loadLabels(context, LABELS_FILE)

        } catch (e: Exception) {
            debug("Fatal Error initializing model: ${e.message}")
        }
    }

    suspend fun detect(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (interpreter == null) return@withContext bitmap

        try {
            // --- 1. Pre-process ---
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                // Note: SSD MobileNet Quantized usually does NOT need NormalizeOp.
                // If using Float model, uncomment: .add(NormalizeOp(127.5f, 127.5f))
                .build()

            val inputTensor = interpreter!!.getInputTensor(0)
            var tensorImage = TensorImage(inputTensor.dataType())
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // --- 2. Prepare Output Buffers ---
            // SSD MobileNet has 4 outputs:
            // 0: Locations [1, 10, 4]
            // 1: Classes   [1, 10]
            // 2: Scores    [1, 10]
            // 3: Count     [1]
            val outputBuffers = mutableMapOf<Int, ByteBuffer>()
            val outputMap = mutableMapOf<Int, Any>()

            for (i in 0 until interpreter!!.outputTensorCount) {
                val tensor = interpreter!!.getOutputTensor(i)
                val buffer = ByteBuffer.allocateDirect(tensor.numBytes())
                buffer.order(ByteOrder.nativeOrder())
                outputMap[i] = buffer
                outputBuffers[i] = buffer
            }

            // --- 3. Run Inference ---
            interpreter?.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputMap)

            // --- 4. Parse Results ---
            val detections = ArrayList<DetectionResult>()

            // Helper to get float from buffer (handles UINT8 or FLOAT32 automatically)
            fun getFloat(index: Int, pos: Int): Float {
                val buffer = outputBuffers[index]!!
                val tensor = interpreter!!.getOutputTensor(index)
                if (tensor.dataType() == DataType.FLOAT32) return buffer.getFloat(pos * 4)

                // De-quantize logic
                val byteVal = buffer.get(pos).toInt() and 0xFF
                val params = tensor.quantizationParams()
                return (byteVal - params.zeroPoint) * params.scale
            }

            // Standard SSD Output Mapping
            // If model is [Boxes, Classes, Scores, Count]
            val count = getFloat(3, 0).toInt().coerceAtMost(10)

            for (i in 0 until count) {
                val score = getFloat(2, i) // Index 2 = Score

                if (score >= MIN_CONFIDENCE) {
                    val classIdx = getFloat(1, i).toInt() // Index 1 = Class

                    // Index 0 = Boxes [top, left, bottom, right]
                    val y1 = getFloat(0, i * 4 + 0) * bitmap.height
                    val x1 = getFloat(0, i * 4 + 1) * bitmap.width
                    val y2 = getFloat(0, i * 4 + 2) * bitmap.height
                    val x2 = getFloat(0, i * 4 + 3) * bitmap.width

                    val label = if (classIdx in labels.indices) labels[classIdx] else "Unknown"
                    val rect = RectF(x1, y1, x2, y2)

                    detections.add(DetectionResult(rect, label, score))
                }
            }

            // --- 5. Draw on Bitmap ---
            return@withContext drawDetections(bitmap, detections)

        } catch (e: Exception) {
            debug("Error during inference: ${e.message}")
            return@withContext bitmap
        }
    }

    private fun drawDetections(bitmap: Bitmap, detections: List<DetectionResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val boxPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textBgPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            alpha = 180 // Semi-transparent
        }

        for (result in detections) {
            val box = result.boundingBox

            // 1. Draw Box
            canvas.drawRect(box, boxPaint)

            // 2. Prepare Text
            val text = "${result.label} ${(result.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            val textHeight = 0f

            // Ensure text doesn't go off screen
            var textX = box.left
            var textY = box.top
            if (textY < textHeight) textY = box.bottom + textHeight

            // 3. Draw Text Background
            canvas.drawRect(
                textX,
                textY - textHeight + 10f,
                textX + textWidth + 20f,
                textY + 10f,
                textBgPaint
            )

            // 4. Draw Text
            canvas.drawText(text, textX + 10f, textY, textPaint)
        }
        return mutableBitmap
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
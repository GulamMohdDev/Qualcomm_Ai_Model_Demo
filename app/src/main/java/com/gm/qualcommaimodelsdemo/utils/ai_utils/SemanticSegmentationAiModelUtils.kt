package com.gm.qualcommaimodelsdemo.utils.ai_utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
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
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SemanticSegmentationAiModelUtils(private val context: Context) : Closeable {

    private val TAG = "SegmentationUtils"

    companion object {
        // ✅ Make sure this matches the filename you downloaded
        private const val MODEL_FILE = "segmentation.tflite"

        // PASCAL VOC 2012 has 21 classes
        private const val NUM_CLASSES = 21

        // Qualcomm DeepLabV3+ MobileNet input size is 513x513
        private const val MODEL_INPUT_SIZE = 513

        // Colors for the 21 classes
        private val CLASS_COLORS = intArrayOf(
            Color.TRANSPARENT,   // 0: Background
            Color.RED,           // 1: Aeroplane
            Color.GREEN,         // 2: Bicycle
            Color.BLUE,          // 3: Bird
            Color.CYAN,          // 4: Boat
            Color.MAGENTA,       // 5: Bottle
            Color.YELLOW,        // 6: Bus
            Color.LTGRAY,        // 7: Car
            Color.DKGRAY,        // 8: Cat
            -0xff0100,           // 9: Chair
            -0xff0001,           // 10: Cow
            -0x100,              // 11: DiningTable
            -0x10000,            // 12: Dog
            -0x100000,           // 13: Horse
            -0xffff01,           // 14: Motorbike
            -0xff00ff,           // 15: Person (Highlight)
            -0x10101,            // 16: PottedPlant
            -0xffff,             // 17: Sheep
            -0xff01,             // 18: Sofa
            -0xff,               // 19: Train
            -0x1000000           // 20: TV
        )
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    private var inputWidth = MODEL_INPUT_SIZE
    private var inputHeight = MODEL_INPUT_SIZE

    init {
        initModel()
    }

    private fun initModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)

            // 1. Initialize GPU Delegate (Crucial for 513x513 resolution)
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
                // Use 4 threads if falling back to CPU
                options.setNumThreads(4)
                interpreter = Interpreter(modelFile, options)
                Log.d(TAG, "Initialized with CPU (GPU failed)")
            }

            // 2. Double check Input Shape from Model
            val inputTensor = interpreter?.getInputTensor(0)
            val shape = inputTensor?.shape()
            if (shape != null && shape.size == 4) {
                inputHeight = shape[1] // Should be 513
                inputWidth = shape[2]  // Should be 513
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing: ${e.message}")
        }
    }

    suspend fun segment(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (interpreter == null) return@withContext bitmap

        try {
            // 1. Pre-process (Same as before)
            val inputBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                bitmap
            }

            // Normalization: MobileNet DeepLab usually uses [-1, 1]
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(127.5f, 127.5f))
                .build()

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(inputBitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 2. Prepare Output
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()

            // Check dimensions to prevent "Index 3" crash
            val hasClassesDimension = (outputShape.size == 4)

            // Dimensions
            val outH = outputShape[1] // 513
            val outW = outputShape[2] // 513
            // If size is 3, we don't have a 4th dim. If 4, we do.
            val outClasses = if (hasClassesDimension) outputShape[3] else 1

            val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes())
            outputBuffer.order(ByteOrder.nativeOrder())

            // 3. Run Inference
            interpreter?.run(tensorImage.buffer, outputBuffer)

            // 4. Post-process
            outputBuffer.rewind()
            val maskPixels = IntArray(outH * outW)
            val isFloat = (outputTensor.dataType() == DataType.FLOAT32)
            val isInt64 = (outputTensor.dataType() == DataType.INT64) // Some models output Long indices

            // --- SCENARIO A: Direct Class Indices [1, 513, 513] ---
            // The model already decided the class. Much faster!
            if (!hasClassesDimension) {
                for (i in 0 until (outH * outW)) {
                    val classIndex: Int = when {
                        isFloat -> outputBuffer.float.toInt()
                        isInt64 -> outputBuffer.long.toInt()
                        else -> outputBuffer.get().toInt() and 0xFF // INT8/UINT8
                    }
                    // Safety check
                    val safeIndex = classIndex % CLASS_COLORS.size
                    maskPixels[i] = CLASS_COLORS[safeIndex]
                }
            }
            // --- SCENARIO B: Probabilities [1, 513, 513, 21] ---
            // We must loop through 21 numbers to find the highest score.
            else {
                for (y in 0 until outH) {
                    for (x in 0 until outW) {
                        var maxScore = -Float.MAX_VALUE
                        var maxClassIndex = 0

                        for (c in 0 until outClasses) {
                            val score: Float = if (isFloat) {
                                outputBuffer.float
                            } else {
                                // De-quantize
                                (outputBuffer.get().toInt() and 0xFF).toFloat()
                            }

                            if (score > maxScore) {
                                maxScore = score
                                maxClassIndex = c
                            }
                        }
                        maskPixels[y * outW + x] = CLASS_COLORS[maxClassIndex % CLASS_COLORS.size]
                    }
                }
            }

            // Create Mask Bitmap
            val maskBitmap = Bitmap.createBitmap(maskPixels, outW, outH, Bitmap.Config.ARGB_8888)

            // Scale back to screen size
            return@withContext Bitmap.createScaledBitmap(maskBitmap, bitmap.width, bitmap.height, true)

        } catch (e: Exception) {
            Log.e(TAG, "Error inference: ${e.message}")
            return@withContext bitmap
        }
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
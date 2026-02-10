package com.gm.qualcommaimodelsdemo.utils.ai_utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.gm.qualcommaimodelsdemo.entity.VideoAction
import com.gm.qualcommaimodelsdemo.logger.Logs
import com.gm.qualcommaimodelsdemo.logger.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue



class VideoClassificationUtils(private val context: Context) : Closeable, Logs {

    override var TAG = "VideoMaeUtils"

    companion object {
        private const val MODEL_FILE = "video_mae.tflite"
        private const val LABELS_FILE = "videomae_labels.txt" // Kinetics-400 labels

        // Standard VideoMAE Config
        private const val NUM_FRAMES = 16  // The model sees 16 frames at once
        private const val INPUT_SIZE = 224 // 224x224 resolution
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var labels: List<String> = emptyList()

    // Dynamic shapes
    private var inputFrames = NUM_FRAMES
    private var inputHeight = INPUT_SIZE
    private var inputWidth = INPUT_SIZE

    // Normalization Stats (ImageNet Mean/Std) - Standard for VideoMAE
    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    init {
        initModel()
    }

    private fun initModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_FILE)

            // 1. Initialize GPU
            try {
                gpuDelegate = GpuDelegate()
                val options = Interpreter.Options()
                options.addDelegate(gpuDelegate)
                interpreter = Interpreter(modelFile, options)
                debug("Initialized with GPU")
            } catch (e: Exception) {
                gpuDelegate?.close()
                gpuDelegate = null
                val options = Interpreter.Options()
                options.setNumThreads(4)
                interpreter = Interpreter(modelFile, options)
                debug("Initialized with CPU")
            }

            // 2. Validate Input Shape [1, 16, 224, 224, 3]
            val inputTensor = interpreter?.getInputTensor(0)
            val shape = inputTensor?.shape()

            // Expected 5D: [Batch, Frames, Height, Width, Channels]
            if (shape != null && shape.size == 5) {
                inputFrames = shape[1]
                inputHeight = shape[2]
                inputWidth = shape[3]
                debug("Model Input: ${inputFrames} frames of ${inputWidth}x${inputHeight}")
            }

            // 3. Load Labels
            labels = FileUtil.loadLabels(context, LABELS_FILE)

        } catch (e: Exception) {
            error("Error initializing: ${e.message}")
        }
    }

    /**
     * Takes a Video URI, extracts frames, and returns the top predicted actions.
     */
    suspend fun classifyVideo(videoUri: Uri): List<VideoAction> = withContext(Dispatchers.Default) {
        if (interpreter == null) return@withContext emptyList()

        try {
            // 1. Extract Frames
            val frames = extractFramesFromVideo(videoUri, inputFrames)
            if (frames.size != inputFrames) {
                error("Failed to extract enough frames. Got ${frames.size}, expected $inputFrames")
                return@withContext emptyList()
            }

            // 2. Prepare 5D Input Buffer
            // Size: 1 * Frames * H * W * 3 (Channels) * 4 (Bytes per Float)
            val bufferSize = 1 * inputFrames * inputHeight * inputWidth * 3 * 4
            val inputBuffer = ByteBuffer.allocateDirect(bufferSize)
            inputBuffer.order(ByteOrder.nativeOrder())

            // 3. Fill Buffer (Normalization & Packing)
            for (bitmap in frames) {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

                val pixels = IntArray(inputWidth * inputHeight)
                scaledBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

                // Loop pixels
                for (pixel in pixels) {
                    val r = Color.red(pixel) / 255.0f
                    val g = Color.green(pixel) / 255.0f
                    val b = Color.blue(pixel) / 255.0f

                    // Apply ImageNet Normalization: (Val - Mean) / Std
                    inputBuffer.putFloat((r - MEAN[0]) / STD[0])
                    inputBuffer.putFloat((g - MEAN[1]) / STD[1])
                    inputBuffer.putFloat((b - MEAN[2]) / STD[2])
                }
                // Don't recycle immediately if you cache them, but here we can
                if (bitmap != scaledBitmap) scaledBitmap.recycle()
            }

            // 4. Run Inference
            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes())
            outputBuffer.order(ByteOrder.nativeOrder())

            interpreter?.run(inputBuffer, outputBuffer)

            // 5. Post-process
            outputBuffer.rewind()
            return@withContext getTopKLabels(outputBuffer)

        } catch (e: Exception) {
            error("Error classifying video: ${e.message}")
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Extracts N evenly spaced frames from a video file.
     */
    private fun extractFramesFromVideo(uri: Uri, targetFrameCount: Int): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frameList = ArrayList<Bitmap>()

        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLong() ?: 0L

            if (durationMs == 0L) return emptyList()

            // Calculate interval to get evenly spaced frames
            val intervalMs = durationMs / targetFrameCount

            for (i in 0 until targetFrameCount) {
                // Calculate time in Microseconds (us)
                val timeUs = (i * intervalMs) * 1000

                // OPTION_CLOSEST ensures we get a frame near that time
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                if (frame != null) {
                    frameList.add(frame)
                } else {
                    // Fallback: If we can't get a frame, duplicate the last one or add black
                    if (frameList.isNotEmpty()) {
                        frameList.add(frameList.last())
                    }
                }
            }
        } catch (e: Exception) {
            error("Frame extraction failed: ${e.message}")
        } finally {
            retriever.release()
        }

        return frameList
    }

    private fun getTopKLabels(buffer: ByteBuffer): List<VideoAction> {
        val pq = PriorityQueue<VideoAction>(3) { o1, o2 -> (o1.score).compareTo(o2.score) }

        // Read probabilities
        for (i in labels.indices) {
            val score = buffer.float
            // Threshold to filter noise
            if (score > 0.05f) {
                val label = if (i < labels.size) labels[i] else "Class $i"
                pq.add(VideoAction(label, score))
                if (pq.size > 3) pq.poll()
            }
        }

        val resultList = ArrayList<VideoAction>()
        while (!pq.isEmpty()) {
            resultList.add(pq.poll())
        }
        return resultList.reversed() // Highest first
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
    }
}
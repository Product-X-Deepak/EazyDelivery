package com.eazydelivery.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.LogUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import timber.log.Timber
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for TensorFlow Lite models
 * Handles loading, caching, and inference with ML models
 */
@Singleton
class MLModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) : Closeable {
    companion object {
        private const val TAG = "MLModelManager"
        private const val SCREEN_ANALYZER_MODEL = "screen_analyzer.tflite"
        private const val TEXT_DETECTOR_MODEL = "text_detector.tflite"
        
        // Model input/output parameters
        private const val IMAGE_SIZE = 224
        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 3
        private const val BATCH_SIZE = 1
        private const val CHANNELS = 3
    }
    
    // Cached interpreters
    private var screenAnalyzerInterpreter: Interpreter? = null
    private var textDetectorInterpreter: Interpreter? = null
    
    // GPU delegate for hardware acceleration
    private var gpuDelegate: GpuDelegate? = null
    
    // Background thread executor for model loading
    private val executor = Executors.newSingleThreadExecutor()
    
    /**
     * Initialize the ML model manager
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Timber.d("Initializing ML model manager")
            
            // Check if GPU acceleration is available
            val compatList = CompatibilityList()
            val useGpu = compatList.isDelegateSupportedOnThisDevice
            
            if (useGpu) {
                Timber.d("GPU acceleration is available, using GPU delegate")
                gpuDelegate = GpuDelegate()
            } else {
                Timber.d("GPU acceleration is not available, using CPU")
            }
            
            // Pre-load models
            loadScreenAnalyzerModel()
            loadTextDetectorModel()
            
            Timber.d("ML model manager initialized")
        } catch (e: Exception) {
            errorHandler.handleException(TAG, e, "Failed to initialize ML model manager")
        }
    }
    
    /**
     * Load the screen analyzer model
     */
    private suspend fun loadScreenAnalyzerModel() = withContext(Dispatchers.IO) {
        try {
            if (screenAnalyzerInterpreter != null) {
                return@withContext
            }
            
            val startTime = SystemClock.elapsedRealtime()
            
            val options = Interpreter.Options().apply {
                if (gpuDelegate != null) {
                    addDelegate(gpuDelegate)
                }
                setNumThreads(4)
            }
            
            val modelFile = FileUtil.loadMappedFile(context, SCREEN_ANALYZER_MODEL)
            screenAnalyzerInterpreter = Interpreter(modelFile, options)
            
            val loadTime = SystemClock.elapsedRealtime() - startTime
            Timber.d("Screen analyzer model loaded in $loadTime ms")
        } catch (e: Exception) {
            errorHandler.handleException(TAG, e, "Failed to load screen analyzer model")
        }
    }
    
    /**
     * Load the text detector model
     */
    private suspend fun loadTextDetectorModel() = withContext(Dispatchers.IO) {
        try {
            if (textDetectorInterpreter != null) {
                return@withContext
            }
            
            val startTime = SystemClock.elapsedRealtime()
            
            val options = Interpreter.Options().apply {
                if (gpuDelegate != null) {
                    addDelegate(gpuDelegate)
                }
                setNumThreads(4)
            }
            
            val modelFile = FileUtil.loadMappedFile(context, TEXT_DETECTOR_MODEL)
            textDetectorInterpreter = Interpreter(modelFile, options)
            
            val loadTime = SystemClock.elapsedRealtime() - startTime
            Timber.d("Text detector model loaded in $loadTime ms")
        } catch (e: Exception) {
            errorHandler.handleException(TAG, e, "Failed to load text detector model")
        }
    }
    
    /**
     * Analyze a screen image to detect UI elements
     * 
     * @param bitmap The screen image to analyze
     * @return The analysis result
     */
    suspend fun analyzeScreen(bitmap: Bitmap): ScreenAnalysisResult = withContext(Dispatchers.Default) {
        try {
            // Ensure model is loaded
            loadScreenAnalyzerModel()
            
            val startTime = SystemClock.elapsedRealtime()
            
            // Preprocess the image
            val inputBuffer = preprocessImage(bitmap)
            
            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(BATCH_SIZE * 10 * FLOAT_TYPE_SIZE)
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // Run inference
            screenAnalyzerInterpreter?.run(inputBuffer, outputBuffer)
            
            // Process results
            outputBuffer.rewind()
            val results = FloatArray(10)
            outputBuffer.asFloatBuffer().get(results)
            
            val inferenceTime = SystemClock.elapsedRealtime() - startTime
            Timber.d("Screen analysis completed in $inferenceTime ms")
            
            // Convert to result object
            return@withContext ScreenAnalysisResult(
                hasAcceptButton = results[0] > 0.7f,
                hasOrderDetails = results[1] > 0.7f,
                hasErrorMessage = results[2] > 0.7f,
                confidence = results[0],
                processingTimeMs = inferenceTime
            )
        } catch (e: Exception) {
            errorHandler.handleException(TAG, e, "Failed to analyze screen")
            return@withContext ScreenAnalysisResult(
                hasAcceptButton = false,
                hasOrderDetails = false,
                hasErrorMessage = false,
                confidence = 0f,
                processingTimeMs = 0,
                error = e.message
            )
        }
    }
    
    /**
     * Preprocess an image for model input
     * 
     * @param bitmap The image to preprocess
     * @return The preprocessed image as a ByteBuffer
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize the bitmap if needed
        val resizedBitmap = if (bitmap.width != IMAGE_SIZE || bitmap.height != IMAGE_SIZE) {
            Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
        } else {
            bitmap
        }
        
        // Allocate a buffer for the input
        val inputBuffer = ByteBuffer.allocateDirect(
            BATCH_SIZE * IMAGE_SIZE * IMAGE_SIZE * CHANNELS * FLOAT_TYPE_SIZE
        )
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()
        
        // Extract RGB values and normalize
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        resizedBitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
        
        for (pixel in pixels) {
            // Extract RGB values
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            
            // Normalize to [-1, 1]
            inputBuffer.putFloat(r * 2 - 1)
            inputBuffer.putFloat(g * 2 - 1)
            inputBuffer.putFloat(b * 2 - 1)
        }
        
        // Clean up if we created a new bitmap
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        
        inputBuffer.rewind()
        return inputBuffer
    }
    
    /**
     * Release resources
     */
    override fun close() {
        try {
            screenAnalyzerInterpreter?.close()
            screenAnalyzerInterpreter = null
            
            textDetectorInterpreter?.close()
            textDetectorInterpreter = null
            
            gpuDelegate?.close()
            gpuDelegate = null
            
            executor.shutdown()
            
            Timber.d("ML model manager resources released")
        } catch (e: Exception) {
            errorHandler.handleException(TAG, e, "Failed to release ML model manager resources")
        }
    }
}

/**
 * Result of screen analysis
 */
data class ScreenAnalysisResult(
    val hasAcceptButton: Boolean,
    val hasOrderDetails: Boolean,
    val hasErrorMessage: Boolean,
    val confidence: Float,
    val processingTimeMs: Long,
    val error: String? = null
)

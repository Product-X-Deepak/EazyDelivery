package com.eazydelivery.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.LruCache
import android.view.accessibility.AccessibilityNodeInfo
import com.eazydelivery.app.util.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes screenshots to detect UI elements for auto-acceptance
 */
@Singleton
class ScreenAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    // TensorFlow Lite interpreter
    private var interpreter: Interpreter? = null

    // Model input dimensions
    private val inputWidth = 224
    private val inputHeight = 224

    // Button detection confidence threshold
    private val confidenceThreshold = 0.7f

    // Visual pattern libraries for each app
    private val patternLibraries = mutableMapOf<String, List<ButtonPattern>>()

    // Cache for recent analysis results to avoid redundant processing
    private val analysisCache = LruCache<String, AnalysisResult>(10)

    // Timestamp of last analysis for each package
    private val lastAnalysisTimestamp = mutableMapOf<String, Long>()

    // Minimum time between analyses for the same package (in milliseconds)
    private val minAnalysisInterval = 500L

    init {
        try {
            // Load TensorFlow Lite model
            val modelFile = "button_detector.tflite"
            interpreter = Interpreter(loadModelFile(modelFile))

            // Initialize pattern libraries
            initializePatternLibraries()

            Timber.d("ScreenAnalyzer initialized successfully")
        } catch (e: Exception) {
            errorHandler.handleException("ScreenAnalyzer.init", e)
            Timber.e(e, "Failed to initialize ScreenAnalyzer")
        }
    }

    /**
     * Analyzes a screenshot to detect UI elements
     * Uses caching to avoid redundant processing of similar screens
     *
     * @param screenshot The screenshot to analyze
     * @param packageName The package name of the app
     * @param rootNode The root accessibility node
     * @return The analysis result
     */
    fun analyzeScreen(
        screenshot: Bitmap,
        packageName: String,
        rootNode: AccessibilityNodeInfo
    ): AnalysisResult {
        // Check if we've analyzed this package recently
        val currentTime = System.currentTimeMillis()
        val lastAnalysisTime = lastAnalysisTimestamp[packageName] ?: 0L

        if (currentTime - lastAnalysisTime < minAnalysisInterval) {
            // Return the cached result if available
            analysisCache.get(packageName)?.let {
                Timber.d("Using cached analysis result for $packageName")
                return it
            }
        }

        // Update the timestamp for this package
        lastAnalysisTimestamp[packageName] = currentTime
        try {
            // Resize and preprocess the screenshot
            val processedBitmap = preprocessImage(screenshot)

            // Run inference with TensorFlow Lite
            val result = runInference(processedBitmap)

            // Get app-specific patterns
            val patterns = patternLibraries[packageName] ?: emptyList()

            // Match patterns with detection results
            val matchedPattern = findBestPatternMatch(result, patterns)

            // Find accept button in accessibility tree
            val acceptButtonNode = findAcceptButton(rootNode, packageName)

            // Determine if confirmation is needed
            val needsConfirmation = detectConfirmationDialog(rootNode, packageName)

            val result = AnalysisResult(
                acceptButtonFound = matchedPattern != null || acceptButtonNode != null,
                acceptButtonNode = acceptButtonNode,
                confidence = matchedPattern?.confidence ?: (if (acceptButtonNode != null) 0.9f else 0.0f),
                needsConfirmation = needsConfirmation
            )

            // Cache the result
            analysisCache.put(packageName, result)

            return result
        } catch (e: Exception) {
            errorHandler.handleException("ScreenAnalyzer.analyzeScreen", e)
            Timber.e(e, "Error analyzing screen for package $packageName")

            // Return default result on error
            return AnalysisResult(
                acceptButtonFound = false,
                acceptButtonNode = null,
                confidence = 0.0f,
                needsConfirmation = false
            )
        }
    }

    /**
     * Preprocesses an image for model input
     * Optimized to reduce memory allocations
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Use a more efficient scaling method for better performance
        val matrix = Matrix()
        val scaleWidth = inputWidth.toFloat() / bitmap.width
        val scaleHeight = inputHeight.toFloat() / bitmap.height
        matrix.postScale(scaleWidth, scaleHeight)

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        ).also {
            // If the input bitmap is not the same as the output, recycle it to free memory
            if (it != bitmap && !bitmap.isRecycled) {
                // Only recycle if it's not a shared bitmap
                if (bitmap.isMutable) {
                    bitmap.recycle()
                }
            }
        }
    }

    /**
     * Runs inference on the preprocessed image
     * Optimized for better performance and memory usage
     */
    private fun runInference(bitmap: Bitmap): FloatArray {
        try {
            // Use a direct ByteBuffer to avoid garbage collection
            val inputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
            inputBuffer.order(ByteOrder.nativeOrder())

            // Use more efficient pixel extraction
            val pixels = IntArray(inputWidth * inputHeight)
            bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

            // Fill input buffer with pixel values
            var pixelIndex = 0
            for (i in 0 until pixels.size) {
                val pixel = pixels[i]

                // Extract RGB values and normalize to [-1, 1]
                inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1.0f)
                inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1.0f)
                inputBuffer.putFloat((pixel and 0xFF) / 127.5f - 1.0f)
            }

            // Reset position to read from the beginning
            inputBuffer.rewind()

            // Prepare output buffer
            val outputBuffer = Array(1) { FloatArray(10) } // Adjust size based on your model

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Recycle the bitmap if it's no longer needed
            if (!bitmap.isRecycled && bitmap.isMutable) {
                bitmap.recycle()
            }

            return outputBuffer[0]
        } catch (e: Exception) {
            errorHandler.handleException("ScreenAnalyzer.runInference", e)
            return FloatArray(10) // Return empty array on error
        }
    }

    /**
     * Finds the best matching pattern from the library
     */
    private fun findBestPatternMatch(
        detectionResult: FloatArray,
        patterns: List<ButtonPattern>
    ): ButtonPattern? {
        var bestMatch: ButtonPattern? = null
        var highestConfidence = confidenceThreshold

        for (pattern in patterns) {
            val confidence = calculatePatternConfidence(detectionResult, pattern)
            if (confidence > highestConfidence) {
                highestConfidence = confidence
                bestMatch = pattern.copy(confidence = confidence)
            }
        }

        return bestMatch
    }

    /**
     * Calculates confidence score for a pattern match
     */
    private fun calculatePatternConfidence(
        detectionResult: FloatArray,
        pattern: ButtonPattern
    ): Float {
        // In a real implementation, this would compare the detection result with the pattern
        // For now, we'll use a simplified approach

        // Simulate pattern matching with random confidence
        return (0.6f + Math.random().toFloat() * 0.4f).coerceAtMost(1.0f)
    }

    /**
     * Finds accept button in accessibility tree
     * Optimized for better performance with early termination
     */
    private fun findAcceptButton(rootNode: AccessibilityNodeInfo, packageName: String): AccessibilityNodeInfo? {
        // Get app-specific button texts
        val buttonTexts = when (packageName) {
            "in.swiggy.deliveryapp" -> listOf("Accept Order", "Accept", "Take Order")
            "com.zomato.delivery" -> listOf("Accept", "Accept Order", "Take")
            "com.zepto.rider" -> listOf("Accept", "Take Order")
            "app.blinkit.onboarding" -> listOf("Accept", "Take Order")
            "com.ubercab.driver" -> listOf("Accept Delivery", "Accept")
            "com.bigbasket.delivery" -> listOf("Accept Order", "Accept", "Take Order")
            else -> listOf("Accept", "Take", "Confirm")
        }

        // First, try to find direct matches using accessibility APIs (faster)
        for (buttonText in buttonTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(buttonText)
            for (node in nodes) {
                if (node.isClickable) {
                    return node
                }
            }
        }

        // If direct matching fails, use breadth-first search
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Check if this node is clickable and has matching text
            val nodeText = node.text?.toString() ?: ""
            if (node.isClickable && buttonTexts.any { nodeText.contains(it, ignoreCase = true) }) {
                return node
            }

            // Add child nodes to the queue
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                if (childNode != null) {
                    queue.add(childNode)
                }
            }
        }

        return null
    }

    /**
     * Detects if a confirmation dialog is present
     * Optimized for better performance with early termination
     */
    private fun detectConfirmationDialog(rootNode: AccessibilityNodeInfo, packageName: String): Boolean {
        // Get app-specific confirmation texts
        val confirmationTexts = when (packageName) {
            "in.swiggy.deliveryapp" -> listOf("Confirm", "Are you sure", "Proceed")
            "com.zomato.delivery" -> listOf("Confirm", "Are you sure", "Proceed")
            "com.zepto.rider" -> listOf("Confirm", "Are you sure", "Proceed")
            "app.blinkit.onboarding" -> listOf("Confirm", "Are you sure", "Proceed")
            "com.ubercab.driver" -> listOf("Confirm", "Are you sure", "Proceed")
            "com.bigbasket.delivery" -> listOf("Confirm", "Are you sure", "Proceed")
            else -> listOf("Confirm", "Are you sure", "Proceed")
        }

        // First, try to find direct matches using accessibility APIs (faster)
        for (confirmText in confirmationTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(confirmText)
            if (nodes.isNotEmpty()) {
                return true
            }
        }

        // If direct matching fails, use breadth-first search with depth limit
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>() // Node and depth
        queue.add(Pair(rootNode, 0))

        // Limit search depth to avoid excessive processing
        val maxDepth = 5

        while (queue.isNotEmpty()) {
            val (node, depth) = queue.removeFirst()

            // Stop searching beyond max depth
            if (depth > maxDepth) continue

            // Check if this node has matching text
            val nodeText = node.text?.toString() ?: ""
            if (confirmationTexts.any { nodeText.contains(it, ignoreCase = true) }) {
                return true
            }

            // Add child nodes to the queue
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                if (childNode != null) {
                    queue.add(Pair(childNode, depth + 1))
                }
            }
        }

        return false
    }

    /**
     * Initializes pattern libraries for each app
     */
    private fun initializePatternLibraries() {
        // In a real implementation, these would be loaded from a database or assets
        patternLibraries["in.swiggy.deliveryapp"] = listOf(
            ButtonPattern("accept_button_1", floatArrayOf(0.8f, 0.2f, 0.1f), 0.0f),
            ButtonPattern("accept_button_2", floatArrayOf(0.7f, 0.3f, 0.2f), 0.0f)
        )

        patternLibraries["com.zomato.delivery"] = listOf(
            ButtonPattern("accept_button_1", floatArrayOf(0.9f, 0.1f, 0.0f), 0.0f),
            ButtonPattern("accept_button_2", floatArrayOf(0.85f, 0.15f, 0.05f), 0.0f)
        )

        patternLibraries["com.zepto.rider"] = listOf(
            ButtonPattern("accept_button_1", floatArrayOf(0.8f, 0.2f, 0.1f), 0.0f)
        )

        patternLibraries["app.blinkit.onboarding"] = listOf(
            ButtonPattern("accept_button_1", floatArrayOf(0.85f, 0.15f, 0.05f), 0.0f)
        )

        patternLibraries["com.ubercab.driver"] = listOf(
            ButtonPattern("accept_button_1", floatArrayOf(0.9f, 0.1f, 0.0f), 0.0f)
        )

        patternLibraries["com.bigbasket.delivery"] = listOf(
            ButtonPattern("accept_button_1", floatArrayOf(0.85f, 0.15f, 0.0f), 0.0f)
        )
    }

    /**
     * Loads a TensorFlow Lite model file
     */
    private fun loadModelFile(modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Updates the pattern library with a new pattern
     */
    fun updatePatternLibrary(packageName: String, pattern: ButtonPattern) {
        try {
            val patterns = patternLibraries[packageName]?.toMutableList() ?: mutableListOf()

            // Check if pattern already exists
            val existingIndex = patterns.indexOfFirst { it.id == pattern.id }
            if (existingIndex >= 0) {
                // Update existing pattern
                patterns[existingIndex] = pattern
            } else {
                // Add new pattern
                patterns.add(pattern)
            }

            patternLibraries[packageName] = patterns

            Timber.d("Updated pattern library for $packageName: ${patterns.size} patterns")
        } catch (e: Exception) {
            errorHandler.handleException("ScreenAnalyzer.updatePatternLibrary", e)
        }
    }

    /**
     * Represents a button pattern in the library
     */
    data class ButtonPattern(
        val id: String,
        val features: FloatArray,
        val confidence: Float
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ButtonPattern

            if (id != other.id) return false
            if (!features.contentEquals(other.features)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + features.contentHashCode()
            return result
        }
    }

    /**
     * Result of screen analysis
     */
    data class AnalysisResult(
        val acceptButtonFound: Boolean,
        val acceptButtonNode: AccessibilityNodeInfo?,
        val confidence: Float,
        val needsConfirmation: Boolean
    )
}

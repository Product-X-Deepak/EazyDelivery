# TensorFlow Lite Integration Guide

This document provides guidance on how TensorFlow Lite is integrated into the EazyDelivery app and how to handle common issues.

## Current Implementation

EazyDelivery uses TensorFlow Lite for machine learning capabilities, including:

1. Order prioritization
2. Text recognition from screenshots
3. Screen analysis for automated interactions

## Dependency Management

TensorFlow Lite dependencies are managed with specific configurations to avoid namespace conflicts:

```kotlin
// TensorFlow Lite with namespace conflict resolution
implementation("org.tensorflow:tensorflow-lite:2.14.0") {
    exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
}
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") {
    exclude(group = "org.tensorflow", module = "tensorflow-lite-gpu-api")
    exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
}
implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
    exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
}
```

Additionally, a global resolution strategy is applied in the root build.gradle.kts:

```kotlin
configurations.all {
    resolutionStrategy {
        // Force a specific version of TensorFlow Lite
        force("org.tensorflow:tensorflow-lite:2.14.0")
        force("org.tensorflow:tensorflow-lite-api:2.14.0")
        
        // Exclude conflicting modules
        exclude(group = "org.tensorflow", module = "tensorflow-lite-gpu-api")
        exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
    }
}
```

## Known Issues and Solutions

### Namespace Conflicts

TensorFlow Lite modules use the same namespace (`org.tensorflow.lite`), which can cause conflicts. This is addressed by:

1. Using consistent versions across all TensorFlow Lite dependencies
2. Applying exclusions for API modules that cause conflicts
3. Using manifest placeholders to provide unique namespaces

### Native Library Loading

TensorFlow Lite requires native libraries. We handle this by:

1. Configuring proper ABI filters in the build.gradle.kts
2. Setting `useLegacyPackaging = true` for jniLibs
3. Excluding TensorFlow native libraries from stripping

### Memory Management

TensorFlow Lite models can consume significant memory. We manage this by:

1. Lazy loading models only when needed
2. Properly closing interpreters when not in use
3. Using a single interpreter instance when possible
4. Implementing proper error handling for out-of-memory situations

## Upgrading TensorFlow Lite

When upgrading TensorFlow Lite:

1. Update all TensorFlow dependencies to the same version
2. Update the forced versions in the root build.gradle.kts
3. Test thoroughly with all ML features
4. Monitor memory usage after the upgrade
5. Update model files if the new version requires it

## Custom Models

EazyDelivery uses custom TensorFlow Lite models located in the assets folder:

- `models/screen_analyzer.tflite`: For analyzing delivery app screens
- `models/text_detector.tflite`: For detecting text in screenshots
- `models/order_prioritizer.tflite`: For prioritizing incoming orders

To update these models:

1. Place the new model file in the assets/models directory
2. Update the model version in MLModelManager.kt
3. Test thoroughly with various inputs

## Performance Considerations

To maintain optimal performance:

1. Use GPU delegation when available
2. Properly size input tensors to match model requirements
3. Batch processing when appropriate
4. Consider quantized models for production
5. Monitor inference time and optimize as needed

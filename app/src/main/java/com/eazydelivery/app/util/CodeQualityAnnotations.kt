package com.eazydelivery.app.util

/**
 * Annotation to mark parameters that are intentionally unused
 * This helps suppress warnings and documents the intention
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Unused(val reason: String = "")

/**
 * Annotation to mark code that needs to be refactored in the future
 */
@Target(
    AnnotationTarget.CLASS, 
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.SOURCE)
annotation class NeedsRefactoring(
    val reason: String,
    val priority: Priority = Priority.MEDIUM,
    val issueId: String = ""
) {
    enum class Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}

/**
 * Annotation to mark code that is kept for backward compatibility
 */
@Target(
    AnnotationTarget.CLASS, 
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.SOURCE)
annotation class KeepForCompatibility(
    val reason: String,
    val untilVersion: String = "",
    val replaceWith: String = ""
)

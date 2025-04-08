package com.eazydelivery.app.util.error

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Sealed class representing different types of application errors
 * This provides a more structured approach to error handling
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /**
     * Network-related errors
     */
    sealed class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {

        /**
         * No internet connection available
         */
        class NoConnection(
            override val message: String = "No internet connection available",
            override val cause: Throwable? = null
        ) : Network(message, cause)

        /**
         * Connection timeout
         */
        class Timeout(
            override val message: String = "Connection timed out",
            override val cause: Throwable? = null
        ) : Network(message, cause)

        /**
         * Server is unreachable
         */
        class ServerUnreachable(
            override val message: String = "Server is unreachable",
            override val cause: Throwable? = null
        ) : Network(message, cause)

        /**
         * Generic network error
         */
        class Generic(
            override val message: String = "Network error occurred",
            override val cause: Throwable? = null
        ) : Network(message, cause)
    }

    /**
     * API-related errors
     */
    sealed class Api(
        override val message: String,
        override val cause: Throwable? = null,
        open val errorCode: Int? = null
    ) : AppError(message, cause) {

        /**
         * Server returned an error response
         */
        class ServerError(
            override val message: String = "Server error occurred",
            override val cause: Throwable? = null,
            override val errorCode: Int? = null
        ) : Api(message, cause, errorCode)

        /**
         * Client error (e.g., 4xx response)
         */
        class ClientError(
            override val message: String = "Client error occurred",
            override val cause: Throwable? = null,
            override val errorCode: Int? = null
        ) : Api(message, cause, errorCode)

        /**
         * Authentication error
         */
        class AuthError(
            override val message: String = "Authentication error",
            override val cause: Throwable? = null,
            override val errorCode: Int? = null
        ) : Api(message, cause, errorCode)

        /**
         * Generic API error
         */
        class Generic(
            override val message: String = "API error occurred",
            override val cause: Throwable? = null,
            override val errorCode: Int? = null
        ) : Api(message, cause, errorCode)
    }

    /**
     * Database-related errors
     */
    sealed class Database(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {

        /**
         * Error reading from database
         */
        class ReadError(
            override val message: String = "Error reading from database",
            override val cause: Throwable? = null
        ) : Database(message, cause)

        /**
         * Error writing to database
         */
        class WriteError(
            override val message: String = "Error writing to database",
            override val cause: Throwable? = null
        ) : Database(message, cause)

        /**
         * Database migration error
         */
        class MigrationError(
            override val message: String = "Database migration error",
            override val cause: Throwable? = null
        ) : Database(message, cause)

        /**
         * Generic database error
         */
        class Generic(
            override val message: String = "Database error occurred",
            override val cause: Throwable? = null
        ) : Database(message, cause)
    }

    /**
     * Permission-related errors
     */
    sealed class Permission(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {

        /**
         * Permission denied by user
         */
        class Denied(
            override val message: String = "Permission denied",
            override val cause: Throwable? = null
        ) : Permission(message, cause)

        /**
         * Permission permanently denied by user
         */
        class PermanentlyDenied(
            override val message: String = "Permission permanently denied",
            override val cause: Throwable? = null
        ) : Permission(message, cause)

        /**
         * Generic permission error
         */
        class Generic(
            override val message: String = "Permission error occurred",
            override val cause: Throwable? = null
        ) : Permission(message, cause)
    }

    /**
     * Feature-specific errors
     */
    sealed class Feature(
        override val message: String,
        override val cause: Throwable? = null,
        open val featureId: String? = null
    ) : AppError(message, cause) {

        /**
         * Feature not available
         */
        class NotAvailable(
            override val message: String = "Feature not available",
            override val cause: Throwable? = null,
            override val featureId: String? = null
        ) : Feature(message, cause, featureId)

        /**
         * Feature requires upgrade
         */
        class RequiresUpgrade(
            override val message: String = "Feature requires upgrade",
            override val cause: Throwable? = null,
            override val featureId: String? = null
        ) : Feature(message, cause, featureId)

        /**
         * Generic feature error
         */
        class Generic(
            override val message: String = "Feature error occurred",
            override val cause: Throwable? = null,
            override val featureId: String? = null
        ) : Feature(message, cause, featureId)
    }

    /**
     * Security-related errors
     */
    sealed class Security(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {

        /**
         * Encryption error
         */
        class EncryptionError(
            override val message: String = "Error encrypting data",
            override val cause: Throwable? = null
        ) : Security(message, cause)

        /**
         * Decryption error
         */
        class DecryptionError(
            override val message: String = "Error decrypting data",
            override val cause: Throwable? = null
        ) : Security(message, cause)

        /**
         * Key generation error
         */
        class KeyGenerationError(
            override val message: String = "Error generating encryption key",
            override val cause: Throwable? = null
        ) : Security(message, cause)

        /**
         * Key retrieval error
         */
        class KeyRetrievalError(
            override val message: String = "Error retrieving encryption key",
            override val cause: Throwable? = null
        ) : Security(message, cause)

        /**
         * Security initialization error
         */
        class InitializationError(
            override val message: String = "Error initializing security components",
            override val cause: Throwable? = null
        ) : Security(message, cause)

        /**
         * Certificate pinning error
         */
        class CertificatePinningError(
            override val message: String = "Certificate pinning validation failed",
            override val cause: Throwable? = null
        ) : Security(message, cause)

        /**
         * Generic security operation error
         */
        class OperationError(
            override val message: String = "Security operation failed",
            override val cause: Throwable? = null
        ) : Security(message, cause)
    }

    /**
     * Unexpected errors
     */
    class Unexpected(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    companion object {
        /**
         * Maps a Throwable to an AppError
         */
        fun from(throwable: Throwable): AppError {
            return when (throwable) {
                // Network errors
                is UnknownHostException -> Network.NoConnection(cause = throwable)
                is SocketTimeoutException -> Network.Timeout(cause = throwable)
                is ConnectException -> Network.ServerUnreachable(cause = throwable)
                is IOException -> Network.Generic(throwable.message ?: "Network error", throwable)

                // If it's already an AppError, return it
                is AppError -> throwable

                // Default to unexpected error
                else -> Unexpected(throwable.message ?: "An unexpected error occurred", throwable)
            }
        }
    }
}

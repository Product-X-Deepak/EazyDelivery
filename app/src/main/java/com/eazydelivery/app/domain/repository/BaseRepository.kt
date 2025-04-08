package com.eazydelivery.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface with common error handling functionality
 */
interface BaseRepository {
    /**
     * Executes a database operation safely and returns the result wrapped in a Result
     */
    suspend fun <T> safeDatabaseOperation(
        operationName: String,
        operation: suspend () -> T
    ): Result<T>
    
    /**
     * Executes a network operation safely and returns the result wrapped in a Result
     */
    suspend fun <T> safeNetworkOperation(
        operationName: String,
        operation: suspend () -> T
    ): Result<T>
    
    /**
     * Creates a flow that safely executes a database operation
     */
    fun <T> safeDatabaseFlow(
        operationName: String,
        operation: suspend () -> T
    ): Flow<Result<T>>
    
    /**
     * Creates a flow that safely executes a network operation
     */
    fun <T> safeNetworkFlow(
        operationName: String,
        operation: suspend () -> T
    ): Flow<Result<T>>
}

package com.eazydelivery.app.domain.repository

import com.eazydelivery.app.util.ErrorHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Base implementation of repository with standardized error handling
 */
abstract class BaseRepositoryImpl(
    protected val errorHandler: ErrorHandler
) : BaseRepository {

    /**
     * Executes a database operation safely and returns the result wrapped in a Result
     */
    override suspend fun <T> safeDatabaseOperation(
        operationName: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: Exception) {
            errorHandler.handleException("DB:$operationName", e)
            Result.failure(e)
        }
    }

    /**
     * Executes a network operation safely and returns the result wrapped in a Result
     */
    override suspend fun <T> safeNetworkOperation(
        operationName: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: Exception) {
            errorHandler.handleException("API:$operationName", e)
            Timber.e(e, "API call failed: $operationName")
            Result.failure(e)
        }
    }

    /**
     * Creates a flow that safely executes a database operation
     */
    override fun <T> safeDatabaseFlow(
        operationName: String,
        operation: suspend () -> T
    ): Flow<Result<T>> = flow {
        emit(safeDatabaseOperation(operationName, operation))
    }.catch { e ->
        errorHandler.handleException("DBFlow:$operationName", e)
        emit(Result.failure(e))
    }

    /**
     * Creates a flow that safely executes a network operation
     */
    override fun <T> safeNetworkFlow(
        operationName: String,
        operation: suspend () -> T
    ): Flow<Result<T>> = flow {
        emit(safeNetworkOperation(operationName, operation))
    }.catch { e ->
        errorHandler.handleException("APIFlow:$operationName", e)
        emit(Result.failure(e))
    }
}

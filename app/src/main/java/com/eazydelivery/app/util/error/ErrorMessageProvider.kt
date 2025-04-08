package com.eazydelivery.app.util.error

import android.content.Context
import com.eazydelivery.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides user-friendly error messages based on AppError types
 */
@Singleton
class ErrorMessageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Gets a user-friendly error message for the given AppError
     * @param error The AppError to get a message for
     * @return A user-friendly error message
     */
    fun getMessage(error: AppError): String {
        return when (error) {
            // Network errors
            is AppError.Network.NoConnection -> context.getString(R.string.error_no_internet_connection)
            is AppError.Network.Timeout -> context.getString(R.string.error_connection_timeout)
            is AppError.Network.ServerUnreachable -> context.getString(R.string.error_server_unreachable)
            is AppError.Network.Generic -> context.getString(R.string.error_network_generic)
            
            // API errors
            is AppError.Api.ServerError -> context.getString(R.string.error_server)
            is AppError.Api.ClientError -> context.getString(R.string.error_client)
            is AppError.Api.AuthError -> context.getString(R.string.error_authentication)
            is AppError.Api.Generic -> context.getString(R.string.error_api_generic)
            
            // Database errors
            is AppError.Database.ReadError -> context.getString(R.string.error_database_read)
            is AppError.Database.WriteError -> context.getString(R.string.error_database_write)
            is AppError.Database.MigrationError -> context.getString(R.string.error_database_migration)
            is AppError.Database.Generic -> context.getString(R.string.error_database_generic)
            
            // Permission errors
            is AppError.Permission.Denied -> context.getString(R.string.error_permission_denied)
            is AppError.Permission.PermanentlyDenied -> context.getString(R.string.error_permission_permanently_denied)
            is AppError.Permission.Generic -> context.getString(R.string.error_permission_generic)
            
            // Feature errors
            is AppError.Feature.NotAvailable -> context.getString(R.string.error_feature_not_available)
            is AppError.Feature.RequiresUpgrade -> context.getString(R.string.error_feature_requires_upgrade)
            is AppError.Feature.Generic -> context.getString(R.string.error_feature_generic)
            
            // Unexpected errors
            is AppError.Unexpected -> error.message.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.error_unexpected)
        }
    }
    
    /**
     * Gets a user-friendly error message with a suggestion for the given AppError
     * @param error The AppError to get a message for
     * @return A user-friendly error message with a suggestion
     */
    fun getMessageWithSuggestion(error: AppError): String {
        val message = getMessage(error)
        val suggestion = getSuggestion(error)
        
        return if (suggestion.isNotBlank()) {
            "$message $suggestion"
        } else {
            message
        }
    }
    
    /**
     * Gets a suggestion for resolving the given AppError
     * @param error The AppError to get a suggestion for
     * @return A suggestion for resolving the error
     */
    fun getSuggestion(error: AppError): String {
        return when (error) {
            // Network errors
            is AppError.Network.NoConnection -> context.getString(R.string.suggestion_check_internet)
            is AppError.Network.Timeout -> context.getString(R.string.suggestion_try_again_later)
            is AppError.Network.ServerUnreachable -> context.getString(R.string.suggestion_check_internet)
            is AppError.Network.Generic -> context.getString(R.string.suggestion_check_internet)
            
            // API errors
            is AppError.Api.ServerError -> context.getString(R.string.suggestion_try_again_later)
            is AppError.Api.ClientError -> context.getString(R.string.suggestion_contact_support)
            is AppError.Api.AuthError -> context.getString(R.string.suggestion_login_again)
            is AppError.Api.Generic -> context.getString(R.string.suggestion_try_again_later)
            
            // Database errors
            is AppError.Database.ReadError -> context.getString(R.string.suggestion_restart_app)
            is AppError.Database.WriteError -> context.getString(R.string.suggestion_restart_app)
            is AppError.Database.MigrationError -> context.getString(R.string.suggestion_update_app)
            is AppError.Database.Generic -> context.getString(R.string.suggestion_restart_app)
            
            // Permission errors
            is AppError.Permission.Denied -> context.getString(R.string.suggestion_grant_permission)
            is AppError.Permission.PermanentlyDenied -> context.getString(R.string.suggestion_app_settings)
            is AppError.Permission.Generic -> context.getString(R.string.suggestion_grant_permission)
            
            // Feature errors
            is AppError.Feature.NotAvailable -> context.getString(R.string.suggestion_update_app)
            is AppError.Feature.RequiresUpgrade -> context.getString(R.string.suggestion_upgrade)
            is AppError.Feature.Generic -> context.getString(R.string.suggestion_try_again_later)
            
            // Unexpected errors
            is AppError.Unexpected -> context.getString(R.string.suggestion_try_again_later)
        }
    }
    
    /**
     * Gets a user-friendly error message for the given Throwable
     * @param throwable The Throwable to get a message for
     * @return A user-friendly error message
     */
    fun getMessage(throwable: Throwable): String {
        val appError = AppError.from(throwable)
        return getMessage(appError)
    }
    
    /**
     * Gets a user-friendly error message with a suggestion for the given Throwable
     * @param throwable The Throwable to get a message for
     * @return A user-friendly error message with a suggestion
     */
    fun getMessageWithSuggestion(throwable: Throwable): String {
        val appError = AppError.from(throwable)
        return getMessageWithSuggestion(appError)
    }
}

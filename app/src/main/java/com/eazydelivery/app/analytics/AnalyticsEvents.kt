package com.eazydelivery.app.analytics

/**
 * Constants for analytics events
 */
object AnalyticsEvents {
    // Screen views
    const val SCREEN_HOME = "screen_home"
    const val SCREEN_ORDERS = "screen_orders"
    const val SCREEN_EARNINGS = "screen_earnings"
    const val SCREEN_SETTINGS = "screen_settings"
    const val SCREEN_PROFILE = "screen_profile"
    const val SCREEN_PLATFORMS = "screen_platforms"
    const val SCREEN_ORDER_DETAILS = "screen_order_details"
    const val SCREEN_NOTIFICATIONS = "screen_notifications"
    const val SCREEN_PERFORMANCE = "screen_performance"
    
    // User actions
    const val ACTION_LOGIN = "login"
    const val ACTION_LOGOUT = "logout"
    const val ACTION_SIGNUP = "signup"
    const val ACTION_RESET_PASSWORD = "reset_password"
    const val ACTION_CHANGE_PASSWORD = "change_password"
    const val ACTION_UPDATE_PROFILE = "update_profile"
    const val ACTION_TOGGLE_PLATFORM = "toggle_platform"
    const val ACTION_TOGGLE_AUTO_ACCEPT = "toggle_auto_accept"
    const val ACTION_TOGGLE_DARK_MODE = "toggle_dark_mode"
    const val ACTION_TOGGLE_NOTIFICATION_SOUND = "toggle_notification_sound"
    const val ACTION_TOGGLE_VIBRATION = "toggle_vibration"
    const val ACTION_SET_MINIMUM_ORDER_AMOUNT = "set_minimum_order_amount"
    const val ACTION_SET_MAXIMUM_DISTANCE = "set_maximum_distance"
    const val ACTION_TOGGLE_ACTIVE_HOURS = "toggle_active_hours"
    const val ACTION_SET_ACTIVE_HOURS = "set_active_hours"
    const val ACTION_BACKUP_DATA = "backup_data"
    const val ACTION_RESTORE_DATA = "restore_data"
    const val ACTION_SHARE_APP = "share_app"
    const val ACTION_RATE_APP = "rate_app"
    const val ACTION_CONTACT_SUPPORT = "contact_support"
    const val ACTION_VIEW_PRIVACY_POLICY = "view_privacy_policy"
    const val ACTION_VIEW_TERMS = "view_terms"
    
    // Order events
    const val EVENT_ORDER_RECEIVED = "order_received"
    const val EVENT_ORDER_ACCEPTED = "order_accepted"
    const val EVENT_ORDER_REJECTED = "order_rejected"
    const val EVENT_ORDER_COMPLETED = "order_completed"
    const val EVENT_ORDER_CANCELLED = "order_cancelled"
    const val EVENT_ORDER_DETAILS_VIEWED = "order_details_viewed"
    
    // Notification events
    const val EVENT_NOTIFICATION_RECEIVED = "notification_received"
    const val EVENT_NOTIFICATION_CLICKED = "notification_clicked"
    const val EVENT_NOTIFICATION_DISMISSED = "notification_dismissed"
    const val EVENT_NOTIFICATION_SETTINGS_CHANGED = "notification_settings_changed"
    
    // Performance events
    const val PERF_SCREEN_ANALYSIS = "screen_analysis"
    const val PERF_DATABASE_QUERY = "database_query"
    const val PERF_NOTIFICATION_PROCESSING = "notification_processing"
    const val PERF_APP_STARTUP = "app_startup"
    const val PERF_SCREEN_LOAD = "screen_load"
    
    // Error events
    const val ERROR_NETWORK = "error_network"
    const val ERROR_DATABASE = "error_database"
    const val ERROR_PERMISSION = "error_permission"
    const val ERROR_SCREEN_ANALYSIS = "error_screen_analysis"
    const val ERROR_NOTIFICATION = "error_notification"
    
    // App lifecycle events
    const val APP_FOREGROUND = "app_foreground"
    const val APP_BACKGROUND = "app_background"
    const val APP_CRASH = "app_crash"
    
    // User properties
    object UserProperties {
        const val USER_TYPE = "user_type"
        const val ACTIVE_PLATFORMS = "active_platforms"
        const val AUTO_ACCEPT_ENABLED = "auto_accept_enabled"
        const val DARK_MODE_ENABLED = "dark_mode_enabled"
        const val NOTIFICATION_SOUND_ENABLED = "notification_sound_enabled"
        const val VIBRATION_ENABLED = "vibration_enabled"
        const val MINIMUM_ORDER_AMOUNT = "minimum_order_amount"
        const val MAXIMUM_DISTANCE = "maximum_distance"
        const val ACTIVE_HOURS_ENABLED = "active_hours_enabled"
        const val APP_VERSION = "app_version"
        const val DEVICE_MODEL = "device_model"
        const val ANDROID_VERSION = "android_version"
    }
    
    // Parameter keys
    object Params {
        const val PLATFORM = "platform"
        const val ORDER_ID = "order_id"
        const val ORDER_AMOUNT = "order_amount"
        const val ORDER_STATUS = "order_status"
        const val NOTIFICATION_ID = "notification_id"
        const val NOTIFICATION_TYPE = "notification_type"
        const val DURATION_MS = "duration_ms"
        const val ERROR_TYPE = "error_type"
        const val ERROR_MESSAGE = "error_message"
        const val SCREEN_NAME = "screen_name"
        const val FEATURE_NAME = "feature_name"
        const val SUCCESS = "success"
        const val REASON = "reason"
    }
}

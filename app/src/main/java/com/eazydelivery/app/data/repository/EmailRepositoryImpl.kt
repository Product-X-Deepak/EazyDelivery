package com.eazydelivery.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.eazydelivery.app.domain.repository.AnalyticsRepository
import com.eazydelivery.app.domain.repository.EmailRepository
import com.eazydelivery.app.util.SecureStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val analyticsRepository: AnalyticsRepository
) : EmailRepository {
    
    companion object {
        private const val KEY_EMAIL = "configured_email"
    }
    
    override suspend fun getConfiguredEmail(): String = withContext(Dispatchers.IO) {
        return@withContext secureStorage.getString(KEY_EMAIL, "")
    }
    
    override suspend fun saveEmail(email: String) = withContext(Dispatchers.IO) {
        secureStorage.saveString(KEY_EMAIL, email)
        Timber.d("Email saved: $email")
    }
    
    override suspend fun sendReport() = withContext(Dispatchers.Main) {
        try {
            val email = getConfiguredEmail()
            if (email.isEmpty()) {
                throw Exception("No email configured")
            }
            
            // Generate report content
            val reportContent = generateReportContent()
            
            // Create email intent
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "EazyDelivery Performance Report")
                putExtra(Intent.EXTRA_TEXT, reportContent)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            Timber.d("Email report sent to $email")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send email report")
            throw e
        }
    }
    
    private suspend fun generateReportContent(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        val todayStats = analyticsRepository.getTodayStats()
        val dailyEarnings = analyticsRepository.getDailyEarnings()
        val weeklyEarnings = analyticsRepository.getWeeklyEarnings()
        val monthlyEarnings = analyticsRepository.getMonthlyEarnings()
        val platformStats = analyticsRepository.getPlatformStats()
        
        val sb = StringBuilder()
        sb.appendLine("EazyDelivery Performance Report")
        sb.appendLine("Generated on: $today")
        sb.appendLine("-----------------------------")
        sb.appendLine()
        
        sb.appendLine("EARNINGS SUMMARY")
        sb.appendLine("Today: ₹$dailyEarnings")
        sb.appendLine("This Week: ₹$weeklyEarnings")
        sb.appendLine("This Month: ₹$monthlyEarnings")
        sb.appendLine()
        
        sb.appendLine("TODAY'S ACTIVITY")
        sb.appendLine("Total Orders: ${todayStats.totalOrders}")
        sb.appendLine("Total Earnings: ₹${todayStats.totalEarnings}")
        sb.appendLine()
        
        sb.appendLine("PLATFORM BREAKDOWN")
        platformStats.forEach { stat ->
            sb.appendLine("${stat.platformName}: ${stat.orderCount} orders, ₹${stat.earnings}")
        }
        sb.appendLine()
        
        sb.appendLine("Thank you for using EazyDelivery!")
        
        return sb.toString()
    }
}


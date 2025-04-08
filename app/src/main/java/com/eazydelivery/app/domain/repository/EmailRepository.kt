package com.eazydelivery.app.domain.repository

interface EmailRepository {
    suspend fun getConfiguredEmail(): String
    suspend fun saveEmail(email: String)
    suspend fun sendReport()
}


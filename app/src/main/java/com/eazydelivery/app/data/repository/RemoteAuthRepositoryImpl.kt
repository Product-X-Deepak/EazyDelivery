package com.eazydelivery.app.data.repository

import com.eazydelivery.app.data.model.LoginRequest
import com.eazydelivery.app.data.model.RegisterRequest
import com.eazydelivery.app.data.remote.ApiService
import com.eazydelivery.app.domain.model.UserInfo
import com.eazydelivery.app.domain.repository.AuthRepository
import com.eazydelivery.app.util.ErrorHandler
import com.eazydelivery.app.util.SecureStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val secureStorage: SecureStorage,
    private val apiService: ApiService,
    private val errorHandler: ErrorHandler
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): UserInfo = withContext(Dispatchers.IO) {
        try {
            // First try to authenticate with Firebase
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Authentication failed")
            
            // Then get additional user data from our API
            val loginRequest = LoginRequest(email, password)
            val apiResponse = apiService.login(loginRequest)
            
            if (!apiResponse.success || apiResponse.data == null) {
                throw Exception(apiResponse.error ?: "API login failed")
            }
            
            val userData = apiResponse.data
            
            // Save user info to secure storage
            secureStorage.saveString(SecureStorage.KEY_USER_ID, userData.id)
            secureStorage.saveString(SecureStorage.KEY_USER_EMAIL, userData.email)
            userData.token?.let { secureStorage.saveString(SecureStorage.KEY_AUTH_TOKEN, it) }
            
            return@withContext UserInfo(
                id = userData.id,
                email = userData.email,
                name = userData.name
            )
        } catch (e: Exception) {
            errorHandler.handleException("AuthRepository.login", e)
            throw e
        }
    }
    
    override suspend fun register(email: String, password: String, name: String): UserInfo = withContext(Dispatchers.IO) {
        try {
            // First register with Firebase
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Registration failed")
            
            // Update Firebase profile with name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Then register with our API
            val registerRequest = RegisterRequest(email, password, name)
            val apiResponse = apiService.register(registerRequest)
            
            if (!apiResponse.success || apiResponse.data == null) {
                throw Exception(apiResponse.error ?: "API registration failed")
            }
            
            val userData = apiResponse.data
            
            // Save user info to secure storage
            secureStorage.saveString(SecureStorage.KEY_USER_ID, userData.id)
            secureStorage.saveString(SecureStorage.KEY_USER_EMAIL, userData.email)
            userData.token?.let { secureStorage.saveString(SecureStorage.KEY_AUTH_TOKEN, it) }
            
            return@withContext UserInfo(
                id = userData.id,
                email = userData.email,
                name = name
            )
        } catch (e: Exception) {
            errorHandler.handleException("AuthRepository.register", e)
            throw e
        }
    }
    
    override suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            
            // Clear user info from secure storage
            secureStorage.remove(SecureStorage.KEY_USER_ID)
            secureStorage.remove(SecureStorage.KEY_USER_EMAIL)
            secureStorage.remove(SecureStorage.KEY_AUTH_TOKEN)
            
            Timber.d("User logged out")
        } catch (e: Exception) {
            errorHandler.handleException("AuthRepository.logout", e)
            throw e
        }
    }
    
    override suspend fun getUserInfo(): UserInfo = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth.currentUser
        
        if (currentUser != null) {
            // Try to get additional user data from API
            try {
                val userId = currentUser.uid
                val apiResponse = apiService.getUserProfile(userId)
                
                if (apiResponse.success && apiResponse.data != null) {
                    val userData = apiResponse.data
                    return@withContext UserInfo(
                        id = userData.id,
                        email = userData.email,
                        name = userData.name
                    )
                }
            } catch (e: Exception) {
                errorHandler.handleException("AuthRepository.getUserInfo", e, "API call failed, falling back to local data")
                // Fall back to Firebase user data
            }
            
            return@withContext UserInfo(
                id = currentUser.uid,
                email = currentUser.email ?: "",
                name = currentUser.displayName ?: ""
            )
        } else {
            val userId = secureStorage.getString(SecureStorage.KEY_USER_ID)
            val userEmail = secureStorage.getString(SecureStorage.KEY_USER_EMAIL)
            
            if (userId.isNotEmpty() && userEmail.isNotEmpty()) {
                return@withContext UserInfo(
                    id = userId,
                    email = userEmail,
                    name = ""
                )
            }
            
            throw Exception("User not logged in")
        }
    }
    
    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        return@withContext firebaseAuth.currentUser != null || 
                secureStorage.getString(SecureStorage.KEY_USER_ID).isNotEmpty()
    }
    
    override suspend fun resetPassword(email: String) = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            
            // Also call our API for password reset
            apiService.resetPassword(email)
            
            Timber.d("Password reset email sent to $email")
        } catch (e: Exception) {
            errorHandler.handleException("AuthRepository.resetPassword", e)
            throw e
        }
    }
}


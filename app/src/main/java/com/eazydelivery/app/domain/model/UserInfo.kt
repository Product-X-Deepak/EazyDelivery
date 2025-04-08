package com.eazydelivery.app.domain.model

data class UserInfo(
    val id: String,
    val phone: String,
    val name: String? = null,
    val email: String? = null,
    val profilePicUrl: String? = null,
    val isNewUser: Boolean = false
)

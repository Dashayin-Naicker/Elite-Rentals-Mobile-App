package com.rentals.eliterentals

data class MessageDto(
    val messageId: Int? = null,
    val senderId: Int,
    val receiverId: Int? =null,
    val messageText: String,
    val timestamp: String? = null,
    val isChatbot: Boolean = false,
    val isBroadcast: Boolean = false,
    val targetRole: String? = null,
    val isEscalated: Boolean = false,
    val language: String? = null
)



package com.example.hello_kotlin.service.model

enum class TransactionType {
    DEBIT,
    CREDIT
}

data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val accountName: String?,
    val rawMessage: String,
    val senderAddress: String,
    val timestamp: Long
)

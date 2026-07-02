package com.example.hello_kotlin.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId"), Index("timestamp")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val senderAddress: String,
    val accountName: String? = null,
    val tagId: Long? = null,
    val rawMessage: String,
    val timestamp: Long,
    val isTagged: Boolean = false,
    val isDeleted: Boolean = false,
    val isIgnored: Boolean = false
)

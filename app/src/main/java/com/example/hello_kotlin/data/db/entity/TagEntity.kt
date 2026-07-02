package com.example.hello_kotlin.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String, // e.g. "#FF6B6B"
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

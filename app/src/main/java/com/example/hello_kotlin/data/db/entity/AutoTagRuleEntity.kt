package com.example.hello_kotlin.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_tag_rules",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId"), Index(value = ["accountKeyword"], unique = true)]
)
data class AutoTagRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountKeyword: String, // e.g. "ZOMATO", "UBER"
    val tagId: Long
)

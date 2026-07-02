package com.example.hello_kotlin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hello_kotlin.data.db.entity.AutoTagRuleEntity
import com.example.hello_kotlin.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

data class AutoTagRuleWithTag(
    val id: Long,
    val accountKeyword: String,
    val tagId: Long,
    val tagName: String,
    val tagColor: String,
    val tagEmoji: String?
)

@Dao
interface AutoTagRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutoTagRuleEntity): Long

    @Query("""
        SELECT r.id, r.accountKeyword, r.tagId, t.name AS tagName, t.colorHex AS tagColor, t.emoji AS tagEmoji
        FROM auto_tag_rules r
        INNER JOIN tags t ON r.tagId = t.id
        ORDER BY r.accountKeyword ASC
    """)
    fun getAllRulesWithTag(): Flow<List<AutoTagRuleWithTag>>

    @Query("DELETE FROM auto_tag_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN auto_tag_rules r ON r.tagId = t.id
        WHERE UPPER(:accountName) LIKE '%' || UPPER(r.accountKeyword) || '%'
        LIMIT 1
    """)
    suspend fun findMatchingTag(accountName: String): TagEntity?

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN auto_tag_rules r ON r.tagId = t.id
        WHERE UPPER(:rawMessage) LIKE '%' || UPPER(r.accountKeyword) || '%'
        LIMIT 1
    """)
    suspend fun findMatchingTagFromMessage(rawMessage: String): TagEntity?
}

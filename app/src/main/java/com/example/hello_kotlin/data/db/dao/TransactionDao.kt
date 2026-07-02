package com.example.hello_kotlin.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hello_kotlin.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class TagTotal(
    val tagId: Long?,
    val tagName: String?,
    val tagColor: String?,
    val tagEmoji: String?,
    val totalAmount: Double
)

data class TransactionWithTag(
    val id: Long,
    val amount: Double,
    val type: String,
    val senderAddress: String,
    val accountName: String?,
    val tagId: Long?,
    val tagName: String?,
    val tagColor: String?,
    val tagEmoji: String?,
    val rawMessage: String,
    val timestamp: Long,
    val isTagged: Boolean
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("UPDATE transactions SET tagId = :tagId, isTagged = 1 WHERE id = :transactionId")
    suspend fun updateTransactionTag(transactionId: Long, tagId: Long)

    @Query("""
        SELECT t.id, t.amount, t.type, t.senderAddress, t.accountName, 
               t.tagId, tag.name AS tagName, tag.colorHex AS tagColor, tag.emoji AS tagEmoji,
               t.rawMessage, t.timestamp, t.isTagged
        FROM transactions t
        LEFT JOIN tags tag ON t.tagId = tag.id
        WHERE t.timestamp >= :startTs AND t.timestamp < :endTs AND t.type = 'DEBIT' AND t.isDeleted = 0 AND t.isIgnored = 0
        ORDER BY t.timestamp DESC
    """)
    fun getDebitTransactionsForMonth(startTs: Long, endTs: Long): Flow<List<TransactionWithTag>>

    @Query("""
        SELECT t.id, t.amount, t.type, t.senderAddress, t.accountName, 
               t.tagId, tag.name AS tagName, tag.colorHex AS tagColor, tag.emoji AS tagEmoji,
               t.rawMessage, t.timestamp, t.isTagged
        FROM transactions t
        LEFT JOIN tags tag ON t.tagId = tag.id
        WHERE t.timestamp >= :startTs AND t.timestamp < :endTs AND t.isDeleted = 0 AND t.isIgnored = 0
        ORDER BY t.timestamp DESC
    """)
    fun getTransactionsForMonth(startTs: Long, endTs: Long): Flow<List<TransactionWithTag>>

    @Query("""
        SELECT t.tagId AS tagId, tag.name AS tagName, tag.colorHex AS tagColor, tag.emoji AS tagEmoji,
               SUM(t.amount) AS totalAmount
        FROM transactions t
        LEFT JOIN tags tag ON t.tagId = tag.id
        WHERE t.timestamp >= :startTs AND t.timestamp < :endTs AND t.type = 'DEBIT' AND t.isDeleted = 0 AND t.isIgnored = 0
        GROUP BY t.tagId
        ORDER BY totalAmount DESC
    """)
    fun getMonthlyTotalByTag(startTs: Long, endTs: Long): Flow<List<TagTotal>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM transactions 
        WHERE timestamp >= :startTs AND timestamp < :endTs AND type = 'DEBIT' AND isDeleted = 0 AND isIgnored = 0
    """)
    fun getTotalExpenditure(startTs: Long, endTs: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM transactions 
        WHERE timestamp >= :startTs AND timestamp < :endTs AND type = 'CREDIT' AND isDeleted = 0 AND isIgnored = 0
    """)
    fun getTotalIncome(startTs: Long, endTs: Long): Flow<Double>

    @Query("""
        SELECT t.id, t.amount, t.type, t.senderAddress, t.accountName, 
               t.tagId, tag.name AS tagName, tag.colorHex AS tagColor, tag.emoji AS tagEmoji,
               t.rawMessage, t.timestamp, t.isTagged
        FROM transactions t
        LEFT JOIN tags tag ON t.tagId = tag.id
        WHERE t.timestamp >= :startTs AND t.timestamp < :endTs AND t.type = 'DEBIT' AND t.isDeleted = 0 AND t.isIgnored = 0
        ORDER BY t.amount DESC
        LIMIT 1
    """)
    fun getTopExpenditure(startTs: Long, endTs: Long): Flow<TransactionWithTag?>

    @Query("SELECT COUNT(*) FROM transactions WHERE isTagged = 0 AND type = 'DEBIT' AND isDeleted = 0 AND isIgnored = 0")
    fun getUntaggedCount(): Flow<Int>

    @Query("""
        SELECT t.id, t.amount, t.type, t.senderAddress, t.accountName, 
               t.tagId, tag.name AS tagName, tag.colorHex AS tagColor, tag.emoji AS tagEmoji,
               t.rawMessage, t.timestamp, t.isTagged
        FROM transactions t
        LEFT JOIN tags tag ON t.tagId = tag.id
        WHERE t.isTagged = 0 AND t.type = 'DEBIT' AND t.isDeleted = 0 AND t.isIgnored = 0
        ORDER BY t.timestamp DESC
    """)
    fun getUntaggedTransactions(): Flow<List<TransactionWithTag>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND isIgnored = 0")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM transactions")
    suspend fun getLatestTransactionTimestamp(): Long?

    @Query("SELECT * FROM transactions WHERE timestamp >= :timestamp")
    suspend fun getTransactionsOnOrAfter(timestamp: Long): List<TransactionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE timestamp >= :startOfDay AND timestamp < :endOfDay AND isDeleted = 0 AND isIgnored = 0)")
    suspend fun hasTransactionsOnDate(startOfDay: Long, endOfDay: Long): Boolean

    @Query("""
        UPDATE transactions 
        SET tagId = :tagId, isTagged = 1 
        WHERE isDeleted = 0 AND isIgnored = 0 AND (UPPER(accountName) LIKE '%' || UPPER(:keyword) || '%' OR UPPER(rawMessage) LIKE '%' || UPPER(:keyword) || '%')
    """)
    suspend fun applyRuleToExistingTransactions(keyword: String, tagId: Long): Int

    @Query("DELETE FROM transactions WHERE senderAddress != 'PASSBOOK'")
    suspend fun deleteSmsTransactions()

    @Query("UPDATE transactions SET isIgnored = 1 WHERE :vendorName != '' AND UPPER(accountName) LIKE '%' || :vendorName || '%'")
    suspend fun markTransactionsAsIgnored(vendorName: String): Int

    @Query("UPDATE transactions SET isIgnored = 0")
    suspend fun resetAllIgnoredTransactions(): Int

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("UPDATE transactions SET rawMessage = :cleanedMessage WHERE id = :id")
    suspend fun updateTransactionRawMessage(id: Long, cleanedMessage: String)
}

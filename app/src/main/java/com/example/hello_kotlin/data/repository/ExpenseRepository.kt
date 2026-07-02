package com.example.hello_kotlin.data.repository

import com.example.hello_kotlin.data.db.dao.AutoTagRuleDao
import com.example.hello_kotlin.data.db.dao.AutoTagRuleWithTag
import com.example.hello_kotlin.data.db.dao.TagDao
import com.example.hello_kotlin.data.db.dao.TagTotal
import com.example.hello_kotlin.data.db.dao.TransactionDao
import com.example.hello_kotlin.data.db.dao.TransactionWithTag
import com.example.hello_kotlin.data.db.entity.AutoTagRuleEntity
import com.example.hello_kotlin.data.db.entity.TagEntity
import com.example.hello_kotlin.data.db.entity.TransactionEntity
import com.example.hello_kotlin.service.model.ParsedTransaction
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val tagDao: TagDao,
    private val autoTagRuleDao: AutoTagRuleDao
) {

    // ── Transactions ──

    /**
     * Insert a parsed transaction. Checks auto-tag rules first:
     * 1. If accountName matches a rule keyword → auto-tag
     * 2. If raw message matches a rule keyword → auto-tag
     * 3. Otherwise → insert as untagged
     *
     * Returns the inserted transaction ID.
     */
    suspend fun insertParsedTransaction(parsed: ParsedTransaction): Long {
        // Try auto-tag by account name first, then by raw message
        val matchingTag = parsed.accountName?.let { autoTagRuleDao.findMatchingTag(it) }
            ?: autoTagRuleDao.findMatchingTagFromMessage(parsed.rawMessage)

        val entity = TransactionEntity(
            amount = parsed.amount,
            type = parsed.type.name,
            senderAddress = parsed.senderAddress,
            accountName = parsed.accountName,
            tagId = matchingTag?.id,
            rawMessage = parsed.rawMessage,
            timestamp = parsed.timestamp,
            isTagged = matchingTag != null
        )
        return transactionDao.insertTransaction(entity)
    }

    suspend fun tagTransaction(transactionId: Long, tagId: Long) {
        transactionDao.updateTransactionTag(transactionId, tagId)
    }

    fun getTransactionsForMonth(startTs: Long, endTs: Long): Flow<List<TransactionWithTag>> {
        return transactionDao.getTransactionsForMonth(startTs, endTs)
    }

    fun getDebitTransactionsForMonth(startTs: Long, endTs: Long): Flow<List<TransactionWithTag>> {
        return transactionDao.getDebitTransactionsForMonth(startTs, endTs)
    }

    fun getMonthlyTotalByTag(startTs: Long, endTs: Long): Flow<List<TagTotal>> {
        return transactionDao.getMonthlyTotalByTag(startTs, endTs)
    }

    fun getTotalExpenditure(startTs: Long, endTs: Long): Flow<Double> {
        return transactionDao.getTotalExpenditure(startTs, endTs)
    }

    fun getTotalIncome(startTs: Long, endTs: Long): Flow<Double> {
        return transactionDao.getTotalIncome(startTs, endTs)
    }

    fun getTopExpenditure(startTs: Long, endTs: Long): Flow<TransactionWithTag?> {
        return transactionDao.getTopExpenditure(startTs, endTs)
    }

    fun getUntaggedCount(): Flow<Int> {
        return transactionDao.getUntaggedCount()
    }

    fun getUntaggedTransactions(): Flow<List<TransactionWithTag>> {
        return transactionDao.getUntaggedTransactions()
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun getLatestTransactionTimestamp(): Long? {
        return transactionDao.getLatestTransactionTimestamp()
    }

    suspend fun getTransactionsOnOrAfter(timestamp: Long): List<com.example.hello_kotlin.data.db.entity.TransactionEntity> {
        return transactionDao.getTransactionsOnOrAfter(timestamp)
    }

    suspend fun hasTransactionsOnDate(startOfDay: Long, endOfDay: Long): Boolean {
        return transactionDao.hasTransactionsOnDate(startOfDay, endOfDay)
    }

    fun getTotalCountFlow(): Flow<Int> {
        return transactionDao.getTotalCountFlow()
    }

    // ── Tags ──

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun getAllTagsList(): List<TagEntity> = tagDao.getAllTagsList()

    suspend fun createTag(name: String, colorHex: String, emoji: String? = null): Long {
        return tagDao.insertTag(TagEntity(name = name, colorHex = colorHex, emoji = emoji))
    }

    suspend fun updateTag(tag: TagEntity) = tagDao.updateTag(tag)

    suspend fun deleteTag(id: Long) = tagDao.deleteTag(id)

    suspend fun getTagById(id: Long): TagEntity? = tagDao.getTagById(id)

    suspend fun getMostUsedTags(limit: Int = 3): List<TagEntity> = tagDao.getMostUsedTags(limit)

    // ── Auto-Tag Rules ──

    fun getAllAutoTagRules(): Flow<List<AutoTagRuleWithTag>> = autoTagRuleDao.getAllRulesWithTag()

    suspend fun addAutoTagRule(keyword: String, tagId: Long): Long {
        return autoTagRuleDao.insertRule(
            AutoTagRuleEntity(accountKeyword = keyword, tagId = tagId)
        )
    }

    suspend fun deleteAutoTagRule(id: Long) = autoTagRuleDao.deleteRule(id)

    suspend fun applyRuleToExistingTransactions(keyword: String, tagId: Long): Int {
        return transactionDao.applyRuleToExistingTransactions(keyword, tagId)
    }

    suspend fun markTransactionsAsIgnored(vendorName: String): Int {
        if (vendorName.isBlank()) return 0
        return transactionDao.markTransactionsAsIgnored(vendorName.uppercase())
    }

    suspend fun resetAllIgnoredTransactions(): Int {
        return transactionDao.resetAllIgnoredTransactions()
    }

    suspend fun getAllTransactionsList(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }

    suspend fun updateTransactionRawMessage(id: Long, cleanedMessage: String) {
        transactionDao.updateTransactionRawMessage(id, cleanedMessage)
    }
}

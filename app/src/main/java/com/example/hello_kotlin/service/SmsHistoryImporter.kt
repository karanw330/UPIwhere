package com.example.hello_kotlin.service

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.example.hello_kotlin.data.repository.ExpenseRepository
import com.example.hello_kotlin.service.model.ParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports historical SMS messages from the device's SMS content provider.
 * Parses each for financial transactions and inserts them into the database.
 */
object SmsHistoryImporter {

    suspend fun importHistoricalMessages(
        context: android.content.Context,
        repository: ExpenseRepository,
        onProgress: (imported: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var importedCount = 0

        val contentResolver = context.contentResolver
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val sortOrder = "date DESC"

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)

        cursor?.use {
            val addressIdx = it.getColumnIndexOrThrow("address")
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val dateIdx = it.getColumnIndexOrThrow("date")
            val total = it.count

            while (it.moveToNext()) {
                val address = it.getString(addressIdx) ?: continue
                val body = it.getString(bodyIdx) ?: continue
                val date = it.getLong(dateIdx)

                val parsed = SmsParser.parse(body, address, date, context)
                if (parsed != null) {
                    try {
                        repository.insertParsedTransaction(parsed)
                        importedCount++
                    } catch (e: Exception) {
                        // Skip duplicates or errors
                    }
                }

                onProgress(importedCount, total)
            }
        }

        importedCount
    }
}

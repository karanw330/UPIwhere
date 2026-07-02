package com.example.hello_kotlin.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.hello_kotlin.service.model.ParsedTransaction
import com.example.hello_kotlin.service.model.TransactionType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object PassbookParser {

    private const val TAG = "PassbookParser"

    // Matches the Google Pay date format: e.g. "02 Jul, 2025" or "11 Aug, 2025"
    private val DATE_PATTERN = Pattern.compile("\\b(\\d{2})\\s+([A-Za-z]{3}),\\s+(\\d{4})\\b")
    
    // Matches Google Pay time format: e.g. "05:37 PM" or "11:58 AM"
    private val TIME_PATTERN = Pattern.compile("\\b(\\d{2}:\\d{2})\\s+(AM|PM)\\b", Pattern.CASE_INSENSITIVE)

    // Matches amount pattern: e.g. "₹26" or "₹202.90"
    private val AMOUNT_PATTERN = Pattern.compile("₹\\s*([\\d,]+(?:\\.\\d{2})?)")

    // Date formatter for Google Pay style: e.g. "02 Jul, 2025"
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    /**
     * Extracts text from the PDF Statement Uri and parses it into ParsedTransactions.
     */
    fun parsePdf(context: Context, uri: Uri): List<ParsedTransaction> {
        val text = extractTextFromPdf(context, uri) ?: return emptyList()
        return parseText(text)
    }

    /**
     * Helper to extract raw text using PDFBox
     */
    fun extractTextFromPdf(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
            null
        }
    }

    /**
     * Parses the raw string statement by segmenting it into date blocks.
     */
    fun parseText(rawText: String): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        
        // Find all indices of date matches in the text
        val matcher = DATE_PATTERN.matcher(rawText)
        val dateMatches = mutableListOf<Pair<String, Int>>() // Pair of (dateString, startPosition)
        
        while (matcher.find()) {
            dateMatches.add(Pair(matcher.group(), matcher.start()))
        }

        if (dateMatches.isEmpty()) {
            return emptyList()
        }

        // Segment the text into blocks between consecutive date matches
        for (i in 0 until dateMatches.size) {
            val dateStr = dateMatches[i].first
            val startIdx = dateMatches[i].second
            val endIdx = if (i + 1 < dateMatches.size) dateMatches[i + 1].second else rawText.length

            val block = rawText.substring(startIdx, endIdx)
            try {
                val parsed = parseBlock(block, dateStr)
                if (parsed != null) {
                    transactions.add(parsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing block: $block", e)
            }
        }

        return transactions
    }

    /**
     * Parses a single segment block representing one transaction.
     */
    private fun parseBlock(block: String, dateStr: String): ParsedTransaction? {
        // Extract Time
        val timeMatcher = TIME_PATTERN.matcher(block)
        if (!timeMatcher.find()) return null
        val timeStr = timeMatcher.group()

        // Combine Date and Time into timestamp
        val timestamp = try {
            val localDate = LocalDate.parse(dateStr, dateFormatter)
            val localTime = LocalTime.parse(timeStr.uppercase(Locale.US), timeFormatter)
            val localDateTime = LocalDateTime.of(localDate, localTime)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date-time: $dateStr $timeStr", e)
            System.currentTimeMillis() // Fallback
        }

        // Extract Amount
        val amountMatcher = AMOUNT_PATTERN.matcher(block)
        if (!amountMatcher.find()) return null
        val amountStr = amountMatcher.group(1).replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // Determine Transaction Type (Only want expenditures, skip received payments)
        val isCredit = block.contains("Received from", ignoreCase = true)
        if (isCredit) return null
        val type = TransactionType.DEBIT

        // Extract Merchant/Recipient Name
        var merchantName: String? = null
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        for (line in lines) {
            if (line.contains("Paid to", ignoreCase = true)) {
                val rawName = line.substringAfter("Paid to", "").trim()
                merchantName = cleanRecipientName(rawName)
                break
            } else if (line.contains("Received from", ignoreCase = true)) {
                val rawName = line.substringAfter("Received from", "").trim()
                merchantName = cleanRecipientName(rawName)
                break
            }
        }

        if (merchantName.isNullOrBlank()) {
            val timeIndex = lines.indexOfFirst { TIME_PATTERN.matcher(it).find() }
            if (timeIndex != -1 && timeIndex + 1 < lines.size) {
                merchantName = cleanRecipientName(lines[timeIndex + 1])
            }
        }

        val singleLineMessage = block.trim().replace(Regex("\\s+"), " ")
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val matcher = emailRegex.toPattern().matcher(singleLineMessage)
        val cleanedMessage = if (matcher.find()) {
            val emailEndIdx = matcher.end()
            singleLineMessage.substring(0, emailEndIdx).trim()
        } else {
            singleLineMessage
        }.removeSuffix(",").trim()

        return ParsedTransaction(
            type = type,
            amount = amount,
            accountName = merchantName ?: "UNKNOWN RECIPIENT",
            rawMessage = cleanedMessage,
            senderAddress = "PASSBOOK",
            timestamp = timestamp
        )
    }

    private fun cleanRecipientName(name: String): String {
        var cleaned = name
        val prefixes = listOf("Mr\\.?\\s+", "Mrs\\.?\\s+", "Ms\\.?\\s+", "Dr\\.?\\s+")
        for (prefix in prefixes) {
            cleaned = cleaned.replace(Regex("^$prefix", RegexOption.IGNORE_CASE), "")
        }
        cleaned = cleaned.replace(Regex("[.,;:!]+$"), "").trim()
        return cleaned.uppercase(Locale.US)
    }
}

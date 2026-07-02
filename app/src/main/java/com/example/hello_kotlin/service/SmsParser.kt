package com.example.hello_kotlin.service

import com.example.hello_kotlin.service.model.ParsedTransaction
import com.example.hello_kotlin.service.model.TransactionType

/**
 * Parses SMS messages to extract financial transaction details.
 * Supports INR (₹, Rs, INR) formats commonly used by Indian banks.
 *
 * Example messages this handles:
 * - "Rs.500.00 debited from A/c XX1234 on 14-Jun-26. UPI/ZOMATO/..."
 * - "INR 1,200.50 has been debited from your account"
 * - "Your a/c XX5678 credited with Rs 5000"
 * - "You've spent Rs. 249 at AMAZON using HDFC card"
 * - "Dear Customer, Rs 150.00 debited from A/C **1234 towards UPI-SWIGGY"
 */
object SmsParser {

    // Debit keywords
    private val DEBIT_PATTERN = Regex(
        "\\b(debited|deducted|spent|paid|withdrawn|purchase[d]?|sent|transferred|debit|payment|dr\\b)",
        RegexOption.IGNORE_CASE
    )

    // Credit keywords
    private val CREDIT_PATTERN = Regex(
        "\\b(credited|received|deposited|refund(?:ed)?|cashback|credit|cr\\b)",
        RegexOption.IGNORE_CASE
    )

    // Amount extraction patterns
    private val AMOUNT_PATTERNS = listOf(
        Regex("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE),
        Regex("(?:debited|deducted|spent|paid|withdrawn|transferred)\\s+(?:by|for|of|with)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)
    )

    // Merchant/account name extraction patterns (multiple strategies)
    private val MERCHANT_PATTERNS = listOf(
        // "trf to MERCHANT Refno" or "trf to MERCHANT Ref"
        Regex("trf\\s+to\\s+([A-Za-z0-9_.\\s]{1,30}?)(?:\\s+(?:Refno|Ref|on|at|\\d))", RegexOption.IGNORE_CASE),
        // "at MERCHANT" or "to MERCHANT" or "from MERCHANT"
        Regex("(?:at|to|from|towards|for|via)\\s+([A-Z][A-Za-z0-9_.\\s]{1,30}?)(?:\\s+(?:on|using|via|ref|UPI|NEFT|IMPS|\\d))", RegexOption.IGNORE_CASE),
        // UPI merchant: "UPI/MERCHANT/" or "UPI-MERCHANT"
        Regex("UPI[/-]([A-Za-z0-9_.]+)", RegexOption.IGNORE_CASE),
        // "towards MERCHANT"
        Regex("towards\\s+([A-Za-z0-9_.\\s]+?)(?:\\s*\\.?\\s*$|\\s+(?:on|ref|txn))", RegexOption.IGNORE_CASE),
        // Generic: after "Info:" or "Ref:"
        Regex("(?:Info|Ref|Desc)[:\\s]+([A-Za-z0-9_.\\s]+?)(?:\\s*$|\\s+\\d)", RegexOption.IGNORE_CASE)
    )

    // OTP / promotional message filter — skip these
    private val SKIP_PATTERNS = listOf(
        Regex("\\bOTP\\b", RegexOption.IGNORE_CASE),
        Regex("\\bverification\\b", RegexOption.IGNORE_CASE),
        Regex("\\bpassword\\b", RegexOption.IGNORE_CASE),
        Regex("\\blogin\\b", RegexOption.IGNORE_CASE),
        Regex("\\bavailable\\s+bal", RegexOption.IGNORE_CASE) // balance-only messages
    )

    // Strict promotional, advertising, and wallet transaction skip patterns
    private val PROMO_SKIP_PATTERNS = listOf(
        Regex("\\b(?:congratulations|congrats|lucky|winner)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:pre-approved|pre approved|pre-qualified|pre qualified|eligible)\\s+(?:for|loan|credit|card|limit|pay\\s*later)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:apply\\s+(?:now|for)|avail|get|instant)\\s+(?:loan|credit|card|limit|pay\\s*later)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:limit\\s+increased|increase\\s+limit|credit\\s+limit|pay\\s*later\\s+limit|limit\\s+enhancement)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:voucher|coupon|promo\\s*code|promocode|discount\\s*code|cashback\\s+offer)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:added\\s+to\\s+wallet|credited\\s+to\\s+wallet|wallet\\s+balance|paytm\\s+wallet|amazon\\s+pay\\s+wallet|wallet\\s+loaded|loaded\\s+in\\s+wallet|add\\s+money)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:limited\\s+period|special|festive|exclusive|deals\\s+of\\s+the\\s+day|flat|off)\\s+offer\\b", RegexOption.IGNORE_CASE)
    )

    /**
     * Parse an SMS body and return a [ParsedTransaction] if it's a financial message,
     * or null if it's not recognized as a transaction.
     */
    fun parse(messageBody: String, senderAddress: String, timestamp: Long = System.currentTimeMillis(), context: android.content.Context? = null): ParsedTransaction? {
        val prefs = context?.getSharedPreferences("expense_tracker", android.content.Context.MODE_PRIVATE)
        val senderPattern = prefs?.getString("sender_pattern", "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK") ?: "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK"
        val excludePattern = prefs?.getString("exclude_pattern", "CBSSBI") ?: "CBSSBI"

        val senderUpper = senderAddress.uppercase()
        val matchSenders = senderPattern.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        val excludeSenders = excludePattern.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }

        // If tracked senders list is specified, address must contain at least one of them
        if (matchSenders.isNotEmpty() && matchSenders.none { senderUpper.contains(it) }) {
            return null
        }

        // If excluded senders list is specified, address must not contain any of them
        if (excludeSenders.any { senderUpper.contains(it) }) {
            return null
        }

        val debitKeywords = prefs?.getString("debit_keywords", "debited,deducted,spent,paid,withdrawn,purchase,sent,transferred,debit,payment,dr") ?: "debited,deducted,spent,paid,withdrawn,purchase,sent,transferred,debit,payment,dr"
        val creditKeywords = prefs?.getString("credit_keywords", "credited,received,deposited,refund,cashback,credit,cr") ?: "credited,received,deposited,refund,cashback,credit,cr"

        val debitPattern = Regex(
            "\\b(${debitKeywords.split(",").map { Regex.escape(it.trim()) }.filter { it.isNotEmpty() }.joinToString("|")})\\b",
            RegexOption.IGNORE_CASE
        )

        val creditPattern = Regex(
            "\\b(${creditKeywords.split(",").map { Regex.escape(it.trim()) }.filter { it.isNotEmpty() }.joinToString("|")})\\b",
            RegexOption.IGNORE_CASE
        )

        val promoKeywords = prefs?.getString("promo_keywords", "congratulations,congrats,lucky,winner,pre-approved,pre approved,pre-qualified,pre qualified,eligible,apply now,apply for,avail,instant loan,pay later,paylater,limit increased,increase limit,credit limit,enhancement,voucher,coupon,promocode,promo code,discount code,cashback offer,added to wallet,credited to wallet,wallet balance,paytm wallet,amazon pay wallet,wallet loaded,add money,limited period,deals of the day,flat off,special offer") ?: "congratulations,congrats,lucky,winner,pre-approved,pre approved,pre-qualified,pre qualified,eligible,apply now,apply for,avail,instant loan,pay later,paylater,limit increased,increase limit,credit limit,enhancement,voucher,coupon,promocode,promo code,discount code,cashback offer,added to wallet,credited to wallet,wallet balance,paytm wallet,amazon pay wallet,wallet loaded,add money,limited period,deals of the day,flat off,special offer"
        val ignoreStrings = prefs?.getString("ignore_strings", "") ?: ""

        // Skip exact ignore strings / phrases
        if (ignoreStrings.isNotEmpty()) {
            val ignores = ignoreStrings.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val bodyLower = messageBody.lowercase()
            if (ignores.any { bodyLower.contains(it) }) {
                return null
            }
        }

        // Skip promotional and wallet-transfer messages unconditionally
        if (promoKeywords.isNotEmpty()) {
            val promoList = promoKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matchedPromo = promoList.any { promoPat ->
                try {
                    Regex(promoPat, RegexOption.IGNORE_CASE).containsMatchIn(messageBody)
                } catch (e: Exception) {
                    messageBody.contains(promoPat, ignoreCase = true)
                }
            }
            if (matchedPromo) {
                return null
            }
        }

        // Skip OTP and promotional messages
        if (SKIP_PATTERNS.any { it.containsMatchIn(messageBody) }) {
            // However, if it also contains amount + debit/credit keywords, don't skip
            val hasAmount = AMOUNT_PATTERNS.any { it.containsMatchIn(messageBody) }
            val hasDebit = debitPattern.containsMatchIn(messageBody)
            if (!hasAmount || !hasDebit) return null
        }

        // Determine transaction type (only do debit, not credit)
        val isDebit = debitPattern.containsMatchIn(messageBody)
        if (!isDebit) return null

        val type = TransactionType.DEBIT

        // Extract amount using AMOUNT_PATTERNS
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(messageBody)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                amount = amountStr.toDoubleOrNull()
                if (amount != null) break
            }
        }
        if (amount == null) return null

        // Sanity check: amount must be positive and reasonable
        if (amount <= 0 || amount > 10_000_000) return null

        // Extract merchant/account name
        val accountName = extractMerchantName(messageBody)

        return ParsedTransaction(
            type = type,
            amount = amount,
            accountName = accountName,
            rawMessage = messageBody,
            senderAddress = senderAddress,
            timestamp = timestamp
        )
    }

    private fun extractMerchantName(message: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(message)
            if (match != null) {
                val name = match.groupValues[1].trim()
                // Clean up: remove trailing punctuation, limit length
                val cleaned = name.replace(Regex("[.,;:!]+$"), "").trim()
                if (cleaned.length >= 2 && cleaned.length <= 50) {
                    return cleaned.uppercase()
                }
            }
        }
        return null
    }

    data class ParseDiagnostic(
        val success: Boolean,
        val skipReason: String?,
        val type: String?,
        val amount: Double?,
        val merchant: String?
    )

    fun diagnose(messageBody: String, senderAddress: String, context: android.content.Context? = null): ParseDiagnostic {
        val prefs = context?.getSharedPreferences("expense_tracker", android.content.Context.MODE_PRIVATE)
        val senderPattern = prefs?.getString("sender_pattern", "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK") ?: "SBIUPI,HDFCBK,ICICIB,AXISBK,KOTAK"
        val excludePattern = prefs?.getString("exclude_pattern", "CBSSBI") ?: "CBSSBI"

        val senderUpper = senderAddress.uppercase()
        val matchSenders = senderPattern.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        val excludeSenders = excludePattern.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }

        if (matchSenders.isNotEmpty() && matchSenders.none { senderUpper.contains(it) }) {
            return ParseDiagnostic(false, "Sender not in tracked list ($senderPattern)", null, null, null)
        }

        if (excludeSenders.any { senderUpper.contains(it) }) {
            return ParseDiagnostic(false, "Sender is excluded ($excludePattern)", null, null, null)
        }

        val debitKeywords = prefs?.getString("debit_keywords", "debited,deducted,spent,paid,withdrawn,purchase,sent,transferred,debit,payment,dr") ?: "debited,deducted,spent,paid,withdrawn,purchase,sent,transferred,debit,payment,dr"
        val creditKeywords = prefs?.getString("credit_keywords", "credited,received,deposited,refund,cashback,credit,cr") ?: "credited,received,deposited,refund,cashback,credit,cr"

        val debitPattern = Regex(
            "\\b(${debitKeywords.split(",").map { Regex.escape(it.trim()) }.filter { it.isNotEmpty() }.joinToString("|")})\\b",
            RegexOption.IGNORE_CASE
        )

        val creditPattern = Regex(
            "\\b(${creditKeywords.split(",").map { Regex.escape(it.trim()) }.filter { it.isNotEmpty() }.joinToString("|")})\\b",
            RegexOption.IGNORE_CASE
        )

        val promoKeywords = prefs?.getString("promo_keywords", "congratulations,congrats,lucky,winner,pre-approved,pre approved,pre-qualified,pre qualified,eligible,apply now,apply for,avail,instant loan,pay later,paylater,limit increased,increase limit,credit limit,enhancement,voucher,coupon,promocode,promo code,discount code,cashback offer,added to wallet,credited to wallet,wallet balance,paytm wallet,amazon pay wallet,wallet loaded,add money,limited period,deals of the day,flat off,special offer") ?: "congratulations,congrats,lucky,winner,pre-approved,pre approved,pre-qualified,pre qualified,eligible,apply now,apply for,avail,instant loan,pay later,paylater,limit increased,increase limit,credit limit,enhancement,voucher,coupon,promocode,promo code,discount code,cashback offer,added to wallet,credited to wallet,wallet balance,paytm wallet,amazon pay wallet,wallet loaded,add money,limited period,deals of the day,flat off,special offer"
        val ignoreStrings = prefs?.getString("ignore_strings", "") ?: ""

        if (ignoreStrings.isNotEmpty()) {
            val ignores = ignoreStrings.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (ignores.any { messageBody.lowercase().contains(it) }) {
                return ParseDiagnostic(false, "Matches custom Ignore Phrase", null, null, null)
            }
        }

        if (promoKeywords.isNotEmpty()) {
            val promoList = promoKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matchedPromo = promoList.find { promoPat ->
                try {
                    Regex(promoPat, RegexOption.IGNORE_CASE).containsMatchIn(messageBody)
                } catch (e: Exception) {
                    messageBody.contains(promoPat, ignoreCase = true)
                }
            }
            if (matchedPromo != null) {
                return ParseDiagnostic(false, "Matches promo keyword/regex: '$matchedPromo'", null, null, null)
            }
        }

        val isDebit = debitPattern.containsMatchIn(messageBody)

        if (!isDebit) {
            return ParseDiagnostic(false, "Not a DEBIT transaction (Credits are skipped)", null, null, null)
        }

        val type = "DEBIT"

        // Extract amount using AMOUNT_PATTERNS
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(messageBody)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                amount = amountStr.toDoubleOrNull()
                if (amount != null) break
            }
        }
        if (amount == null) {
            return ParseDiagnostic(false, "Could not extract amount (patterns: Rs/INR/₹ or debited/spent/paid by)", null, null, null)
        }

        val accountName = extractMerchantName(messageBody)

        return ParseDiagnostic(true, null, type, amount, accountName)
    }
}

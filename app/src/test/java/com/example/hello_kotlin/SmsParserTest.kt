package com.example.hello_kotlin

import com.example.hello_kotlin.service.SmsParser
import com.example.hello_kotlin.service.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun parse_validDebitMessage_returnsParsedTransaction() {
        val message = "Dear Customer, Rs.500.00 debited from A/C XX1234 on 14-Jun-26 towards UPI-ZOMATO."
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("ZOMATO", result.accountName)
    }

    @Test
    fun parse_creditMessage_returnsNull() {
        val message = "Your a/c XX5678 credited with Rs 5000.00 on 14-Jun-26. Info: SALARY."
        val result = SmsParser.parse(message, "SBIUPI")
        assertNull(result)
    }

    @Test
    fun parse_sbiDebitMessage_returnsParsedTransaction() {
        val message = "Dear UPI user A/C XX1234 debited by 28.00 on date 29Mar26 trf to RAMESH KUMAR Refno 60123456789 If not u? call-1800111101 for other services-1800111102-SBI"
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result!!.type)
        assertEquals(28.0, result.amount, 0.001)
        assertEquals("RAMESH KUMAR", result.accountName)
    }

    @Test
    fun parse_otpMessage_returnsNull() {
        val message = "Your OTP for login to SBI banking is 482910. Do not share this password with anyone."
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNull(result)
    }

    @Test
    fun parse_preApprovedPromoMessage_returnsNull() {
        val message = "Congratulations! You are eligible for a pre-approved loan of Rs 5,00,000. Apply now by clicking http://sbi.co.in/loan"
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNull(result)
    }

    @Test
    fun parse_limitIncreasePromoMessage_returnsNull() {
        val message = "Great news! Your HDFC credit card limit has been increased by Rs 50000. Avail now by clicking here."
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNull(result)
    }

    @Test
    fun parse_walletAddedMessage_returnsNull() {
        val message = "Congratulations! Rs 100 cashback has been credited to your Paytm wallet."
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNull(result)
    }

    @Test
    fun parse_walletLoadPromoMessage_returnsNull() {
        val message = "Add money to your Amazon Pay wallet and get flat 50 off coupon. Limited period offer."
        val result = SmsParser.parse(message, "SBIUPI")
        
        assertNull(result)
    }

    @Test
    fun parse_nonMatchSender_returnsNull() {
        // Tracker defaults to matches like "SBIUPI" and rejects others
        val message = "Rs.500.00 debited from A/C XX1234"
        val result = SmsParser.parse(message, "UNKNOWN_SENDER")
        
        assertNull(result)
    }
}
